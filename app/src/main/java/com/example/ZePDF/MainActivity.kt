package com.example.ZePDF

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.ZePDF.databinding.ActivityMainBinding
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import com.litehalls.mupdf.viewer.DocumentActivity

/**
 * MainActivity do Zé PDF — agora usa MuPDF (Chaka) como viewer principal.
 *
 * O Chaka oferece:
 * - Seleção de texto nativa com toque longo (handles arrastáveis)
 * - Zoom com pinça, scroll contínuo
 * - Busca de texto, bookmarks, sumário
 * - Modo noturno, paleta de cores
 * - Suporte a PDF, EPUB, MOBI, DOCX, etc.
 *
 * As funções de edição (salvar cópia, exportar, compactar, senha, combinar)
 * continuam usando PDFBox.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // URI do PDF atualmente aberto
    private var pdfUri: Uri? = null

    // Launcher para abrir um único PDF via SAF
    private val abrirDocumento =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: SecurityException) { }
                pdfUri = it
                abrirPdfNoChaka(it)
            }
        }

    // Launcher para selecionar múltiplos PDFs (combinar)
    private val abrirMultiplosPdfs =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris != null && uris.size >= 2) {
                combinarPdfs(uris)
            } else {
                Toast.makeText(this, "Selecione pelo menos 2 PDFs", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.bigOpenButton.setOnClickListener {
            abrirDocumento.launch(arrayOf("application/pdf"))
        }
        supportActionBar?.title = "Zé PDF"

        // Inicializa PDFBox (usado para edição/manipulação)
        PDFBoxResourceLoader.init(applicationContext)

        // Se o app foi aberto via "Abrir com..." de outro app
        tratarIntentDeAbrirArquivo(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        // Deixa o botão de sair em vermelho, com destaque
        menu?.findItem(R.id.action_sair)?.icon?.let {
            it.mutate()
            it.setTint(Color.RED)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sair -> {
                finishAffinity() // fecha o app por completo (todas as telas/tasks)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // ==================== ABRIR PDF NO CHAKA (MuPDF) ====================

    /**
     * Abre o PDF no Chaka (MuPDF viewer).
     * O Chaka oferece:
     * - Toque longo em texto → seleção nativa com handles
     * - Toque longo em área em branco → bookmark
     * - Zoom, scroll, busca, sumário, etc.
     */
    private fun abrirPdfNoChaka(uri: Uri) {
        try {
            val intent = Intent(this, DocumentActivity::class.java)
            intent.action = Intent.ACTION_VIEW
            intent.data = uri
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)   // <- ESSENCIAL
            // Habilita modo Custom UI (toolbar do Chaka integrada)
            intent.putExtra(DocumentActivity.EXTRA_USE_CUSTOM_UI, true)
            intent.putExtra(
                DocumentActivity.EXTRA_VISIBLE_BUTTONS,
                arrayOf(
                    DocumentActivity.BUTTON_SEARCH,
                    DocumentActivity.BUTTON_SHARE,
                    DocumentActivity.BUTTON_COPY
                )
            )
            startActivity(intent)
        } catch (e: Exception) {
            // SEM fallback genérico aqui — isso causava o loop infinito.
            // Por enquanto, só mostra o erro exato pra gente diagnosticar.
            Toast.makeText(this, "Erro ao abrir PDF: ${e.javaClass.simpleName}: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ==================== BUSCA DE TEXTO (via PDFBox) ====================

    private fun buscarTextoNoPdf() {
        val uri = pdfUri
        if (uri == null) {
            Toast.makeText(this, "Abra um PDF primeiro", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(this).apply {
            hint = "Digite o texto a buscar"
            setPadding(32, 32, 32, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Buscar no PDF")
            .setView(input)
            .setPositiveButton("Buscar") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotEmpty()) {
                    executarBusca(uri, query)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun executarBusca(uri: Uri, query: String) {
        try {
            contentResolver.openInputStream(uri).use { inputStream ->
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val totalPaginas = document.numberOfPages
                var encontrado = false

                for (i in 1..totalPaginas) {
                    stripper.startPage = i
                    stripper.endPage = i
                    val texto = stripper.getText(document)
                    if (texto.contains(query, ignoreCase = true)) {
                        encontrado = true
                        Toast.makeText(this, "Encontrado na página $i", Toast.LENGTH_SHORT).show()
                        // Reabre o Chaka na página encontrada (se possível)
                        abrirPdfNoChaka(uri)
                        break
                    }
                }
                document.close()
                if (!encontrado) {
                    Toast.makeText(this, "Texto não encontrado", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro na busca: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ==================== EXTRAÇÃO DE TEXTO ====================

    private fun extrairTextoDaPagina(uri: Uri) {
        try {
            contentResolver.openInputStream(uri).use { inputStream ->
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val textoExtraido = stripper.getText(document)
                document.close()
                mostrarTextoSelecionavel(textoExtraido)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao extrair texto: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarTextoSelecionavel(texto: String) {
        val textoFinal = texto.ifBlank {
            "Não foi encontrado texto selecionável neste documento."
        }

        val editText = EditText(this).apply {
            setText(textoFinal)
            isFocusable = true
            isFocusableInTouchMode = true
            setTextIsSelectable(true)
            setPadding(32, 32, 32, 32)
            background = null
        }

        AlertDialog.Builder(this)
            .setTitle("Texto do documento")
            .setMessage("Toque e segure para selecionar e copiar")
            .setView(android.widget.ScrollView(this).apply { addView(editText) })
            .setPositiveButton("Fechar", null)
            .show()
    }

    // ==================== COMPARTILHAR ====================

    private fun compartilharPdfOriginal() {
        val uri = pdfUri
        if (uri == null) {
            Toast.makeText(this, "Abra um PDF primeiro", Toast.LENGTH_SHORT).show()
            return
        }
        compartilharUri(uri, "application/pdf")
    }

    private fun converterPaginaEmImagemECompartilhar(uri: Uri) {
        try {
            contentResolver.openInputStream(uri).use { inputStream ->
                val document = PDDocument.load(inputStream)
                val pdfRenderer = android.graphics.pdf.PdfRenderer(
                    contentResolver.openFileDescriptor(uri, "r")!!
                )

                val pageIndex = 0
                pdfRenderer.openPage(pageIndex).use { page ->
                    val bitmap = Bitmap.createBitmap(
                        page.width * 3,
                        page.height * 3,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val pasta = File(getExternalFilesDir(null), "imagens")
                    pasta.mkdirs()
                    val arquivoImagem = File(pasta, "pagina_${pageIndex + 1}_${System.currentTimeMillis()}.png")
                    FileOutputStream(arquivoImagem).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    val uriImagem = FileProvider.getUriForFile(
                        this,
                        "$packageName.fileprovider",
                        arquivoImagem
                    )
                    compartilharUri(uriImagem, "image/png")
                }
                pdfRenderer.close()
                document.close()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao gerar imagem: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun compartilharUri(uri: Uri, tipoMime: String) {
        val intentCompartilhar = Intent(Intent.ACTION_SEND).apply {
            type = tipoMime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intentCompartilhar, "Compartilhar via"))
    }

    // ==================== SALVAR CÓPIA ====================

    private fun salvarCopia(uri: Uri) {
        try {
            contentResolver.openInputStream(uri).use { inputStream ->
                val document = PDDocument.load(inputStream)
                val pastaSaida = getExternalFilesDir(null)
                val arquivoSaida = File(pastaSaida, "copia_${System.currentTimeMillis()}.pdf")
                FileOutputStream(arquivoSaida).use { out ->
                    document.save(out)
                }
                document.close()
                Toast.makeText(this, "Cópia salva em: ${arquivoSaida.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar cópia: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ==================== EXPORTAR COMO... ====================

    private fun mostrarDialogExportar(uri: Uri) {
        val opcoes = arrayOf("Texto (.txt)", "Imagem PNG", "Imagem JPEG")
        AlertDialog.Builder(this)
            .setTitle("Exportar como...")
            .setItems(opcoes) { _, which ->
                when (which) {
                    0 -> exportarComoTexto(uri)
                    1 -> exportarPaginaComoImagem(uri, Bitmap.CompressFormat.PNG, "png")
                    2 -> exportarPaginaComoImagem(uri, Bitmap.CompressFormat.JPEG, "jpg")
                }
            }
            .show()
    }

    private fun exportarComoTexto(uri: Uri) {
        try {
            contentResolver.openInputStream(uri).use { inputStream ->
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val texto = stripper.getText(document)
                document.close()

                val pasta = getExternalFilesDir(null)
                val arquivo = File(pasta, "exportado_${System.currentTimeMillis()}.txt")
                OutputStreamWriter(FileOutputStream(arquivo)).use { writer ->
                    writer.write(texto)
                }
                Toast.makeText(this, "Texto salvo em: ${arquivo.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao exportar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportarPaginaComoImagem(uri: Uri, formato: Bitmap.CompressFormat, extensao: String) {
        try {
            val pdfRenderer = android.graphics.pdf.PdfRenderer(
                contentResolver.openFileDescriptor(uri, "r")!!
            )
            val pageIndex = 0
            pdfRenderer.openPage(pageIndex).use { page ->
                val bitmap = Bitmap.createBitmap(
                    page.width * 3,
                    page.height * 3,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val pasta = getExternalFilesDir(null)
                val arquivo = File(pasta, "pagina_${pageIndex + 1}_${System.currentTimeMillis()}.$extensao")
                FileOutputStream(arquivo).use { out ->
                    bitmap.compress(formato, 100, out)
                }
                Toast.makeText(this, "Imagem salva em: ${arquivo.absolutePath}", Toast.LENGTH_LONG).show()
            }
            pdfRenderer.close()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao exportar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ==================== COMPACTAR PDF ====================

    private fun compactarPdf(uri: Uri) {
        try {
            contentResolver.openInputStream(uri).use { inputStream ->
                val document = PDDocument.load(inputStream)
                val pastaSaida = getExternalFilesDir(null)
                val arquivoSaida = File(pastaSaida, "compactado_${System.currentTimeMillis()}.pdf")
                FileOutputStream(arquivoSaida).use { out ->
                    document.save(out)
                }
                document.close()

                val tamanhoOriginal = contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 0
                val tamanhoNovo = arquivoSaida.length()
                val reducao = if (tamanhoOriginal > 0) {
                    "Redução: ${((tamanhoOriginal - tamanhoNovo) * 100 / tamanhoOriginal)}%"
                } else ""

                Toast.makeText(this, "PDF compactado! $reducao\nSalvo em: ${arquivoSaida.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao compactar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ==================== DEFINIR SENHA ====================

    private fun mostrarDialogDefinirSenha(uri: Uri) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val inputSenha = EditText(this).apply {
            hint = "Senha"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val inputConfirmar = EditText(this).apply {
            hint = "Confirmar senha"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(inputSenha)
        layout.addView(inputConfirmar)

        AlertDialog.Builder(this)
            .setTitle("Definir senha do PDF")
            .setView(layout)
            .setPositiveButton("Aplicar") { _, _ ->
                val senha = inputSenha.text.toString()
                val confirmar = inputConfirmar.text.toString()
                if (senha.isNotEmpty() && senha == confirmar) {
                    aplicarSenhaPdf(uri, senha)
                } else {
                    Toast.makeText(this, "Senhas não conferem ou estão vazias", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun aplicarSenhaPdf(uri: Uri, senha: String) {
        try {
            contentResolver.openInputStream(uri).use { inputStream ->
                val document = PDDocument.load(inputStream)
                val accessPermission = AccessPermission()
                val protectionPolicy = StandardProtectionPolicy(senha, senha, accessPermission)
                protectionPolicy.encryptionKeyLength = 128
                document.protect(protectionPolicy)

                val pastaSaida = getExternalFilesDir(null)
                val arquivoSaida = File(pastaSaida, "protegido_${System.currentTimeMillis()}.pdf")
                FileOutputStream(arquivoSaida).use { out ->
                    document.save(out)
                }
                document.close()
                Toast.makeText(this, "PDF protegido salvo em: ${arquivoSaida.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao proteger PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ==================== COMBINAR ARQUIVOS ====================

    private fun combinarPdfs(uris: List<Uri>) {
        try {
            val documentoFinal = PDDocument()
            for (uri in uris) {
                contentResolver.openInputStream(uri).use { inputStream ->
                    val doc = PDDocument.load(inputStream)
                    for (page in doc.pages) {
                        documentoFinal.addPage(page)
                    }
                    doc.close()
                }
            }

            val pastaSaida = getExternalFilesDir(null)
            val arquivoSaida = File(pastaSaida, "combinado_${System.currentTimeMillis()}.pdf")
            FileOutputStream(arquivoSaida).use { out ->
                documentoFinal.save(out)
            }
            documentoFinal.close()
            Toast.makeText(this, "PDFs combinados! Salvo em: ${arquivoSaida.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao combinar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ==================== UTILITÁRIOS ====================

    private fun tratarIntentDeAbrirArquivo(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) { }
                pdfUri = uri
                abrirPdfNoChaka(uri)
            }
        }
    }
}