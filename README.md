# Zé PDF

Aplicativo Android de leitura de arquivos PDF (e outros formatos de documento), com renderização e seleção de texto nativa via **MuPDF**.

## Funcionalidades

- Abertura de arquivos PDF pelo app ou via "Abrir com..." de outros apps/gerenciadores de arquivos
- Renderização de páginas com zoom e rolagem contínua (MuPDF)
- Seleção de texto por toque longo, com handles arrastáveis
- Botão de copiar flutuante próximo à seleção, além do botão fixo na barra superior
- Busca de texto dentro do documento
- Compartilhamento do arquivo original
- Interface do leitor customizada (barra superior sempre visível, com apenas os botões essenciais: fechar, buscar, compartilhar e copiar)
- Funcionalidades adicionais de manipulação de PDF via PDFBox (salvar cópia, exportar, compactar, definir senha, combinar múltiplos PDFs)

## Tecnologias

- **Kotlin** — Activity principal e integração com o leitor
- **[MuPDF](https://mupdf.com/)** (módulo `mupdf-lib`) — renderização de páginas e seleção de texto
- **[PDFBox Android](https://github.com/TomRoush/PdfBox-Android)** — operações de edição/manipulação de PDF
- **AndroidX / Material Components**

## Estrutura do projeto

```
ZePDF/
├── app/                 # Módulo principal (MainActivity, tela inicial, operações com PDFBox)
└── mupdf-lib/            # Módulo do leitor MuPDF (DocumentActivity, ReaderView, PageView)
```

## Como compilar

1. Clone o repositório
2. Abra no Android Studio
3. Sincronize o Gradle
4. Rode a configuração `app` em um dispositivo/emulador (minSdk 21, compileSdk/targetSdk 34)

## Licenciamento

Este projeto integra a biblioteca **MuPDF**, distribuída pela Artifex Software sob a licença **GNU AGPL v3** (ou licença comercial alternativa oferecida pela Artifex). Ao distribuir este aplicativo publicamente, revise os termos da AGPL em [mupdf.com/licensing](https://mupdf.com/licensing.html) para garantir conformidade.

## Status

Projeto em desenvolvimento ativo.
