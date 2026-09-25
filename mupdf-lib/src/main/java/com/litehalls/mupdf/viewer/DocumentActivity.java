package com.litehalls.mupdf.viewer;

import com.artifex.mupdf.fitz.SeekableInputStream;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.method.PasswordTransformationMethod;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.widget.ListPopupWindow;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DocumentActivity extends AppCompatActivity
{
	/* The core rendering instance */
	enum TopBarMode {Main, Search, More};

	// Intent extras for custom UI configuration
	public static final String EXTRA_USE_CUSTOM_UI = "com.litehalls.mupdf.viewer.USE_CUSTOM_UI";
	public static final String EXTRA_DOC_TYPE = "com.litehalls.mupdf.viewer.DOC_TYPE";
	public static final String EXTRA_VISIBLE_BUTTONS = "com.litehalls.mupdf.viewer.VISIBLE_BUTTONS";
	public static final String EXTRA_CUSTOM_MENU_ITEMS = "com.litehalls.mupdf.viewer.CUSTOM_MENU_ITEMS";
	public static final String EXTRA_CALLBACK_KEY = "com.litehalls.mupdf.viewer.CALLBACK_KEY";
	public static final String EXTRA_CUSTOM_SHARE = "com.litehalls.mupdf.viewer.CUSTOM_SHARE";
	public static final String EXTRA_SHOW_VOTE = "com.litehalls.mupdf.viewer.SHOW_VOTE";
	public static final String EXTRA_VOTE_COUNT = "com.litehalls.mupdf.viewer.VOTE_COUNT";
	public static final String EXTRA_IS_VOTED = "com.litehalls.mupdf.viewer.IS_VOTED";

	// Button identifiers for EXTRA_VISIBLE_BUTTONS
	public static final String BUTTON_COPY = "copy";
	public static final String BUTTON_OUTLINE = "outline";
	public static final String BUTTON_SINGLE_COLUMN = "singleColumn";
	public static final String BUTTON_TEXT_LEFT = "textLeft";
	public static final String BUTTON_FLIP_VERTICAL = "flipVertical";
	public static final String BUTTON_LOCK = "lock";
	public static final String BUTTON_CROP_MARGIN = "cropMargin";
	public static final String BUTTON_FOCUS = "focus";
	public static final String BUTTON_SMART_FOCUS = "smartFocus";
	public static final String BUTTON_COLOR = "color";
	public static final String BUTTON_LAYOUT = "layout";
	public static final String BUTTON_LINK = "link";
	public static final String BUTTON_SEARCH = "search";
	public static final String BUTTON_SHARE = "share";
	public static final String BUTTON_HELP = "help";

	private MuPDFCore    core;
	private String       mDocTitle;
	private String       mDocKey;
	private ReaderView   mDocView;
	private View         mButtonsView;
	private boolean      mButtonsVisible;
	private EditText     mPasswordView;
	private TextView     mDocNameView;
	private TextView     mDocTypeView;
	private ImageButton  mCloseButton;
	private Button mFloatingCopyButton;
	private SeekBar      mPageSlider;
	private int          mPageSliderRes;
	private TextView     mPageNumberView;
	private boolean      mUseCustomUI = false;
	private String       mDocType;
	private String[]     mVisibleButtons;
	private CustomMenuItem[] mCustomMenuItems;
	private String       mCallbackKey;
	private boolean      mUseCustomShare = false;
	private DocumentActivityCallback mCallback;
	
	// Vote of relevance
	private View         mVoteSection;
	private ImageButton  mThumbsUpButton;
	private TextView     mVoteCountView;
	private boolean      mShowVote = false;
	private int          mVoteCount = 0;
	private boolean      mIsVoted = false;
	
	// Floating message card
	private View         mFloatingMessageCard;
	private TextView     mFloatingMessageText;
	private ImageButton  mCopyButton;
	private ImageButton  mSingleColumnButton;
	private ImageButton  mTextLeftButton;
	private ImageButton  mFlipVerticalButton;
	private ImageButton  mLockButton;
	private ImageButton  mCropMarginButton;
	private ImageButton  mFocusButton;
	private ImageButton  mSmartFocusButton;
	private ImageButton  mColorButton;
	private ImageButton  mShareButton;
	private ImageButton  mHelpButton;
	private ImageButton  mSearchButton;
	private ImageButton  mOutlineButton;
	private ImageButton  mOverflowButton;
	private ViewAnimator mTopBarSwitcher;
	private ImageButton  mLinkButton;
	private TopBarMode   mTopBarMode = TopBarMode.Main;
    private ImageButton  mSearchClear;
	private ImageButton  mSearchBack;
	private ImageButton  mSearchFwd;
	private ImageButton  mSearchClose;
	private EditText     mSearchText;
	private SearchTask   mSearchTask;
	private AlertDialog.Builder mAlertBuilder;
	
	// Custom navigation/zoom bar
	private View         mCustomNavZoomBar;
	private View         mBottomControlContainer;
	private ImageButton  mPagePrevButton;
	private ImageButton  mPageNextButton;
	private TextView     mPageNavText;
	private ImageButton  mZoomInButton;
	private ImageButton  mZoomOutButton;
	private TextView     mZoomLevelText;
    private boolean    mSingleColumnHighlight = false;
    private boolean    mTextLeftHighlight = false;
    private boolean    mFlipVerticalHighlight = false;
    private boolean    mLockHighlight = false;
    private boolean    mCropMarginHighlight = false;
    private boolean    mFocusHighlight = false;
    private boolean    mSmartFocusHighlight = false;
	private boolean    mLinkHighlight = false;
	private final Handler mHandler = new Handler(Looper.getMainLooper());
	private ArrayList<TocItem> mFlatOutline;
	private boolean mReturnToLibraryActivity = false;
    private boolean mNavigationBar;

    /**
     * if navigation bar is hidden. ime will bring it up, cause docview resize,
     * result in page number changes in flowable documents.
     */
    // to notify onSizeChanged ime change by user
    private boolean mKeyboardChanged = false;
    // to notify setOnSystemUiVisibilityChangeListener ime change by user
    private boolean mKeyboardChanged2 = false;
    // to notify onSizeChanged ime close by system button (this is a guess)
    private boolean mKeyboardChanged3 = false;
    private boolean mOrientationChanged = false;
    private int mOrientation;
    private int lastPage = -1;
    private long firstClickTime = 0;
    private boolean single = false;
    private boolean leftText = false;
    private boolean vertical = false;
    private Uri uri;
    private String mMimeType;

    private int highlightColor = Color.argb(0xFF, 0x3C, 0xB3, 0x71);
    private int highunlightColor = Color.argb(0xFF, 255, 255, 255);
    private int enabledColor = Color.argb(255, 255, 255, 255);
    private int disabledColor = Color.argb(255, 128, 128, 128);

	protected int mDisplayDPI;
	private int mLayoutEM;      // read from prefs
	private int mLayoutW = 312;
	private int mLayoutH = 504;

	protected View mLayoutButton;
	protected PopupMenu mLayoutPopupMenu;
	protected ListPopupWindow mColorPopupWindow;

	private MuPDFCore openBuffer(byte buffer[], String magic)
	{
		try
		{
			core = new MuPDFCore(this, buffer, magic);
		}
		catch (Exception e)
		{
			Tool.e("Error opening document buffer: " + e);
			return null;
		}
		return core;
	}

	private MuPDFCore openStream(SeekableInputStream stm, String magic)
	{
		try
		{
			core = new MuPDFCore(this, stm, magic);
		}
		catch (Exception e)
		{
			Tool.e("Error opening document stream: " + e);
			return null;
		}
		return core;
	}

	private MuPDFCore openCore(Uri uri, long size, String mimetype) throws IOException {
		ContentResolver cr = getContentResolver();

		Tool.i("Opening document " + uri);

		InputStream is = cr.openInputStream(uri);
		byte[] buf = null;
		int used = -1;
		try {
			final int limit = 8 * 1024 * 1024;
			if (size < 0) { // size is unknown
				buf = new byte[limit];
				used = is.read(buf);
				boolean atEOF = is.read() == -1;
				if (used < 0 || (used == limit && !atEOF)) // no or partial data
					buf = null;
			} else if (size <= limit) { // size is known and below limit
				buf = new byte[(int) size];
				used = is.read(buf);
				if (used < 0 || used < size) // no or partial data
					buf = null;
			}
			if (buf != null && buf.length != used) {
				byte[] newbuf = new byte[used];
				System.arraycopy(buf, 0, newbuf, 0, used);
				buf = newbuf;
			}
		} catch (OutOfMemoryError e) {
			buf = null;
		} finally {
			is.close();
		}

		if (buf != null) {
			Tool.i("  Opening document from memory buffer of size " + buf.length);
			return openBuffer(buf, mimetype);
		} else {
			Tool.i("  Opening document from stream");
			return openStream(new ContentInputStream(cr, uri, size), mimetype);
		}
	}

	private void showCannotOpenDialog(String reason) {
		AlertDialog alert = mAlertBuilder.create();
		alert.setTitle(String.format(Locale.ROOT, getResources().getString(R.string.cannot_open_document_Reason), reason));
		alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
		alert.show();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Tool.create(this);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

		DisplayMetrics metrics = getResources().getDisplayMetrics();
		// DisplayMetrics metrics = new DisplayMetrics();
		// getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mDisplayDPI = (int)metrics.densityDpi;

		mAlertBuilder = new AlertDialog.Builder(this, R.style.MyDialog);

		setUserCss();

		if (core == null) {
			if (savedInstanceState != null && savedInstanceState.containsKey("DocTitle")) {
				mDocTitle = savedInstanceState.getString("DocTitle");
			}
		}
		if (core == null) {
			Intent intent = getIntent();

			mReturnToLibraryActivity = intent.getIntExtra(getComponentName().getPackageName() + ".ReturnToLibraryActivity", 0) != 0;

			// Read custom UI configuration
			mUseCustomUI = intent.getBooleanExtra(EXTRA_USE_CUSTOM_UI, false);
			mDocType = intent.getStringExtra(EXTRA_DOC_TYPE);
			mVisibleButtons = intent.getStringArrayExtra(EXTRA_VISIBLE_BUTTONS);
			
			// Handle Parcelable array properly
			android.os.Parcelable[] parcelables = intent.getParcelableArrayExtra(EXTRA_CUSTOM_MENU_ITEMS);
			if (parcelables != null) {
				mCustomMenuItems = new CustomMenuItem[parcelables.length];
				for (int i = 0; i < parcelables.length; i++) {
					mCustomMenuItems[i] = (CustomMenuItem) parcelables[i];
				}
			}
			
			mCallbackKey = intent.getStringExtra(EXTRA_CALLBACK_KEY);
			mUseCustomShare = intent.getBooleanExtra(EXTRA_CUSTOM_SHARE, false);
			
			// Vote section extras
			mShowVote = intent.getBooleanExtra(EXTRA_SHOW_VOTE, false);
			mVoteCount = intent.getIntExtra(EXTRA_VOTE_COUNT, 0);
			mIsVoted = intent.getBooleanExtra(EXTRA_IS_VOTED, false);
			
			// Retrieve callback if provided
			if (mCallbackKey != null) {
				mCallback = DocumentCallbackRegistry.getInstance().getCallback(mCallbackKey);
			}

			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				uri = intent.getData();
				String mimetype = getIntent().getType();

				if (uri == null)  {
					showCannotOpenDialog("No document uri to open");
					return;
				}

				mDocKey = uri.toString();

				Tool.i("OPEN URI " + uri.toString());
				Tool.i("  MAGIC (Intent) " + mimetype);

				// in case of cbz recognized as zip
				// .gz recognized as */*
				if ("application/zip".equals(mimetype) || "*/*".equals(mimetype)) {
					mimetype = null;
				}

				mDocTitle = null;
				long size = -1;
				Cursor cursor = null;

				if (intent.hasExtra(Intent.EXTRA_TITLE))
					mDocTitle = intent.getStringExtra(Intent.EXTRA_TITLE);

				if (mDocTitle == null) {
					try {
						cursor = getContentResolver().query(uri, null, null, null, null);
						if (cursor != null && cursor.moveToFirst()) {
							int idx;

							idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
							if (idx >= 0 && cursor.getType(idx) == Cursor.FIELD_TYPE_STRING)
								mDocTitle = cursor.getString(idx);

							idx = cursor.getColumnIndex(OpenableColumns.SIZE);
							if (idx >= 0 && cursor.getType(idx) == Cursor.FIELD_TYPE_INTEGER)
								size = cursor.getLong(idx);

							if (size == 0)
								size = -1;
						}
					} catch (Exception x) {
						// Ignore any exception and depend on default values for title
						// and size (unless one was decoded
					} finally {
						if (cursor != null)
							cursor.close();
					}
				}

				Tool.i("  NAME " + mDocTitle);
				Tool.i("  SIZE " + size);

				if (mimetype == null || mimetype.equals("application/octet-stream")) {
					mimetype = getContentResolver().getType(uri);
					Tool.i("  MAGIC (Resolved) " + mimetype);
				}

				mMimeType = mimetype;

				if (mimetype == null || mimetype.equals("application/octet-stream")) {
					mimetype = mDocTitle;
					Tool.i("  MAGIC (Filename) " + mimetype);
				}

				try {
					core = openCore(uri, size, mimetype);
					SearchTaskResult.set(null);
				} catch (Exception x) {
					showCannotOpenDialog(x.toString());
					return;
				}

				if (core != null) {
					String docTitle = core.getTitle();
					if (docTitle != null && !"".equals(docTitle)) {
						// only set if user didn't provide a title
						if(intent.getStringExtra(Intent.EXTRA_TITLE) == null)
							mDocTitle = docTitle;
					}
					String fileDigest = Tool.getDigest(getContentResolver(), uri);
					mDocKey = mDocTitle + "." + String.valueOf(size) + "." + fileDigest;

					// Auto-detect document type if not provided
					if (mDocType == null && mUseCustomUI) {
						mDocType = detectDocumentType(mMimeType, mDocTitle);
					}
				}
			}
			if (core != null && core.needsPassword()) {
				requestPassword(savedInstanceState);
				return;
			}
			if (core != null && core.countPages() == 0)
			{
				core = null;
			}
		}
		if (core == null)
		{
			AlertDialog alert = mAlertBuilder.create();
			alert.setTitle(R.string.cannot_open_document);
			alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
			alert.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();
				}
			});
			alert.show();
			return;
		}

		createUI(savedInstanceState);
		mOrientation = getResources().getConfiguration().orientation;
		HelpActivity.delay = 600;
		OutlineActivity.content = core.hasOutline() ? OutlineActivity.CONTENTS.TOC : OutlineActivity.CONTENTS.BOOKMARK;
		BookmarkRepository.getInstance().setup(core, mDocKey);
	}

	private String detectDocumentType(String mimeType, String filename) {
		if (mimeType != null) {
			if (mimeType.contains("pdf")) return "PDF";
			if (mimeType.contains("epub")) return "EPUB";
			if (mimeType.contains("mobi")) return "MOBI";
			if (mimeType.contains("word") || mimeType.contains("document")) return "DOC";
			if (mimeType.contains("presentation") || mimeType.contains("powerpoint")) return "PPT";
			if (mimeType.contains("spreadsheet") || mimeType.contains("excel")) return "XLS";
			if (mimeType.contains("text")) return "TXT";
			if (mimeType.contains("html")) return "HTML";
			if (mimeType.contains("cbz") || mimeType.contains("comic")) return "CBZ";
			if (mimeType.contains("fb2")) return "FB2";
			if (mimeType.contains("xps")) return "XPS";
			if (mimeType.contains("zip")) return "ZIP";
			if (mimeType.contains("gzip")) return "GZIP";
		}
		
		// Fallback to filename extension
		if (filename != null) {
			String lower = filename.toLowerCase();
			if (lower.endsWith(".pdf")) return "PDF";
			if (lower.endsWith(".epub")) return "EPUB";
			if (lower.endsWith(".mobi")) return "MOBI";
			if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "DOC";
			if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) return "PPT";
			if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return "XLS";
			if (lower.endsWith(".txt")) return "TXT";
			if (lower.endsWith(".html") || lower.endsWith(".htm")) return "HTML";
			if (lower.endsWith(".cbz")) return "CBZ";
			if (lower.endsWith(".fb2")) return "FB2";
			if (lower.endsWith(".xps")) return "XPS";
			if (lower.endsWith(".zip")) return "ZIP";
			if (lower.endsWith(".gz")) return "GZIP";
		}
		
		return "DOCUMENT";
	}


	private void setUserCss() {
		StringBuilder sb = new StringBuilder();
		sb.append("@page{margin:2em}");
		sb.append("body{margin:0;padding:0;overflow-wrap:break-word;}");
		sb.append("p{margin:0.6em 0;}");
		sb.append("table{border-collapse:collapse;}");
		com.artifex.mupdf.fitz.Context.setUserCSS(sb.toString());
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Tool.fullScreen(getWindow());
	}

	public void requestPassword(final Bundle savedInstanceState) {
		mPasswordView = new EditText(this);
		mPasswordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
		mPasswordView.setTransformationMethod(new PasswordTransformationMethod());

		AlertDialog alert = mAlertBuilder.create();
		alert.setTitle(R.string.enter_password);
		alert.setView(mPasswordView);
		alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.okay),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (core.authenticatePassword(mPasswordView.getText().toString())) {
							createUI(savedInstanceState);
						} else {
							requestPassword(savedInstanceState);
						}
					}
				});
		alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
				new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
		alert.show();
	}

	public void relayoutDocument() {
		core.layout(mDocView.mCurrent, mLayoutW, mLayoutH, mLayoutEM);
	}

	public void afterRelayout(int loc) {
		mFlatOutline = null;
		mDocView.mHistory.clear();
		mDocView.refresh();

        if (lastPage > -1 ) {
            HelpActivity.updateReadme();
            if (lastPage < core.countPages())
                loc = lastPage;
            lastPage = -1;
        }

		BookmarkRepository.getInstance().onSizeChange();
		mDocView.setDisplayedViewIndex(loc);

		if (vertical) {
			vertical = false;
			toggleFlipVerticalHighlight();
		}
	}

	public void createUI(Bundle savedInstanceState) {
		if (core == null)
			return;

		// Now create the UI.
		// First create the document view
		mDocView = new ReaderView(this) {
			@Override
			protected void onMoveToChild(int i) {
				if (core == null)
					return;

		        updatePageNumView(i);
                updatePageSlider(i);
				super.onMoveToChild(i);
			}

			@Override
			protected void onTapMainDocArea() {
				if (!mButtonsVisible) {
					showButtons();
				} else {
					// if (mTopBarMode == TopBarMode.Main)
						hideButtons();
				}
			}

			@Override
			protected void onDocMotion() {
				hideButtons();
			}

			@Override
			public void onSizeChanged(int w, int h, int oldw, int oldh) {
                // ime changed by user
                if (mKeyboardChanged) {
                    mKeyboardChanged = false;
                    if (!mOrientationChanged) return;
                }

                // ime closed by system button (a guess)
                if (mKeyboardChanged3) {
                    mKeyboardChanged3 = false;
                    if (!mOrientationChanged) return;
                }

                // ajust doc name width
                mHandler.postDelayed(new Runnable(){
                    public void run() {
                        updateTopBar(w);
                    }}, 200);

				if (core.isReflowable()) {
					mLayoutW = w * 72 * 2 / mDisplayDPI;
					mLayoutH = h * 72 * 2 / mDisplayDPI;
					relayoutDocument();
				} else {
					refresh();
				}
			}
		};
		mDocView.setAdapter(new PageAdapter(this, core));

		mSearchTask = new SearchTask(this, core) {
			@Override
			protected void onTextFound(SearchTaskResult result) {
				SearchTaskResult.set(result);
				// Ask the ReaderView to move to the resulting page
				mDocView.setDisplayedViewIndex(result.pageNumber);
				// Make the ReaderView act on the change to SearchTaskResult
				// via overridden onChildSetup method.
				mDocView.resetupChildren();
			}
		};

		// Make the buttons overlay, and store all its
		// controls in variables
		makeButtonsView();

        // below android 8 (api26)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            TooltipCompat.setTooltipText(mCopyButton, getString(R.string.copy));
            TooltipCompat.setTooltipText(mSingleColumnButton, getString(R.string.single_column));
            TooltipCompat.setTooltipText(mTextLeftButton, getString(R.string.text_left));
            TooltipCompat.setTooltipText(mFlipVerticalButton, getString(R.string.flip_vertical));
            TooltipCompat.setTooltipText(mLockButton, getString(R.string.lock));
            TooltipCompat.setTooltipText(mCropMarginButton, getString(R.string.crop_margin));
            TooltipCompat.setTooltipText(mFocusButton, getString(R.string.focus));
            TooltipCompat.setTooltipText(mSmartFocusButton, getString(R.string.smart_focus));
            TooltipCompat.setTooltipText(mLinkButton, getString(R.string.link));
            TooltipCompat.setTooltipText(mSearchButton, getString(R.string.text_search));
            TooltipCompat.setTooltipText(mLayoutButton, getString(R.string.format_size));
            TooltipCompat.setTooltipText(mColorButton, getString(R.string.color));
            TooltipCompat.setTooltipText(mShareButton, getString(R.string.share));
            TooltipCompat.setTooltipText(mOutlineButton, getString(R.string.contents));
            TooltipCompat.setTooltipText(mHelpButton, getString(R.string.help));
            if (mCloseButton != null) {
                TooltipCompat.setTooltipText(mCloseButton, getString(R.string.dismiss));
            }
            if (mOverflowButton != null) {
                TooltipCompat.setTooltipText(mOverflowButton, getString(R.string.more));
            }
        }

		// Set up the page slider
		int smax = Math.max(core.countPages()-1,1);
		mPageSliderRes = ((10 + smax - 1)/smax) * 2;

		// Set the file-name text
		mDocNameView.setText(mDocTitle);
		TooltipCompat.setTooltipText(mDocNameView, mDocNameView.getText());

		// Activate the seekbar
		mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {
				mDocView.pushHistory();
                if (!mTextLeftHighlight)
				    mDocView.setDisplayedViewIndex((seekBar.getProgress()+mPageSliderRes/2)/mPageSliderRes);
                else
				    mDocView.setDisplayedViewIndex(core.countPages() - 1 - (seekBar.getProgress()+mPageSliderRes/2)/mPageSliderRes);
			}

			public void onStartTrackingTouch(SeekBar seekBar) {}

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
                if (!mTextLeftHighlight)
				    updatePageNumView((progress+mPageSliderRes/2)/mPageSliderRes);
                else
				    updatePageNumView(core.countPages() - 1 - (progress+mPageSliderRes/2)/mPageSliderRes);
			}
		});

        mDocNameView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                long secondClickTime = System.currentTimeMillis();;

                if (secondClickTime - firstClickTime < ViewConfiguration.getDoubleTapTimeout()) {
                    exit();
                }
                firstClickTime = secondClickTime;
            }
        });

        mCopyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                copy();
            }
        });

        if (core.isReflowable()) {
            mSingleColumnButton.setVisibility(View.GONE);
        }
        else {
            mSingleColumnButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggleSingleColumnHighlight();
                }
            });
        }

        if (core.isReflowable()) {
            mTextLeftButton.setVisibility(View.GONE);
        }
        else {
            mTextLeftButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggleTextLeftHighlight();
                }
            });
        }

        mFlipVerticalButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleFlipVerticalHighlight();
            }
        });

        mLockButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleLock();
            }
        });

        mCropMarginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleCropMargin();
            }
        });

        mFocusButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleFocus();
            }
        });

        if (core.isReflowable()) {
            mSmartFocusButton.setVisibility(View.GONE);
        }
        else {
            mSmartFocusButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggleSmartFocus();
                }
            });
        }

        makeColorPopupWindow();

        mColorButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mColorPopupWindow.setAnchorView(v);
                mColorPopupWindow.show();
            }
        });

        mShareButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Check if custom share handler is enabled
                if (mUseCustomShare && mCallback != null) {
                    boolean handled = mCallback.onShareDocument(DocumentActivity.this, uri, mDocTitle);
                    if (handled) {
                        return; // Custom handler took care of it
                    }
                }
                
                // Default share behavior
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.setType(mMimeType);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_to)));
            }
        });

        mHelpButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(DocumentActivity.this, HelpActivity.class);
                startActivity(intent);
            }
        });

		// Activate the search-preparing button
		mSearchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				searchModeOn();
			}
		});

		mSearchClose.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				searchModeOff();
			}
		});

		// Search invoking buttons are disabled while there is no text specified
		setButtonEnabled(mSearchBack, false);
		setButtonEnabled(mSearchFwd, false);
		setButtonEnabled(mSearchClear, false);

		// React to interaction with the text widget
		mSearchText.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				boolean haveText = s.toString().trim().length() > 0;
				setButtonEnabled(mSearchBack, haveText);
				setButtonEnabled(mSearchFwd, haveText);
				setButtonEnabled(mSearchClear, haveText);

                if (!haveText) {
                    mSearchText.requestFocus();
                    showKeyboard();
                }

				// Remove any previous search results
				if (SearchTaskResult.get() != null && !mSearchText.getText().toString().trim().equals(SearchTaskResult.get().txt)) {
					SearchTaskResult.set(null);
					mDocView.resetupChildren();
				}
			}
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {}
		});

		//React to Done button on keyboard
		mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE)
					search(1);
				return false;
			}
		});

		mSearchText.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER)
					search(1);
				return false;
			}
		});

        mSearchText.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                case MotionEvent.ACTION_UP:
                    mSearchText.requestFocus();
                    showKeyboard();
                    return true;
                default:
                    break;
                }
                return false;
            }
        });

        mSearchClear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSearchText.setText("");
            }
        });

		// Activate search invoking buttons
		mSearchBack.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				search(-1);
			}
		});

		mSearchFwd.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				search(1);
			}
		});

		mLinkButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setLinkHighlight(!mLinkHighlight);
			}
		});

		if (core.isReflowable()) {
			mLayoutButton.setVisibility(View.VISIBLE);
			mLayoutPopupMenu = new PopupMenu(this, mLayoutButton);
			mLayoutPopupMenu.getMenuInflater().inflate(R.menu.layout_menu, mLayoutPopupMenu.getMenu());
			mLayoutPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					float oldLayoutEM = mLayoutEM;
					int id = item.getItemId();
					if (id == R.id.action_layout_8pt) mLayoutEM = 8;
					else if (id == R.id.action_layout_10pt) mLayoutEM = 10;
					else if (id == R.id.action_layout_12pt) mLayoutEM = 12;
					else if (id == R.id.action_layout_14pt) mLayoutEM = 14;
					else if (id == R.id.action_layout_16pt) mLayoutEM = 16;
					else if (id == R.id.action_layout_18pt) mLayoutEM = 18;
					else if (id == R.id.action_layout_20pt) mLayoutEM = 20;
					if (oldLayoutEM != mLayoutEM) {
						relayoutDocument();
			            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			            SharedPreferences.Editor edit = prefs.edit();
                        edit.putInt("layoutem"+mDocKey, mLayoutEM);
			            edit.apply();
                    }
					return true;
				}
			});
			mLayoutButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
                    Menu menu = mLayoutPopupMenu.getMenu();
                    for (int mi = 0; mi < menu.size(); mi++) {
                        MenuItem item = menu.getItem(mi);
                        item.setCheckable(false);
                        String title = item.getTitle().toString();
                        if (title.equals(String.valueOf(mLayoutEM) + "pt")) {
                            item.setCheckable(true).setChecked(true);
                        }
                    }
					mLayoutPopupMenu.show();
				}
			});
		}

        mOutlineButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(DocumentActivity.this, OutlineActivity.class);

                if (mFlatOutline == null)
                    if (core.hasOutline())
                        mFlatOutline = core.getOutline();

                if (mFlatOutline != null) {
                    Bundle bundle = new Bundle();
                    bundle.putInt("POSITION", mDocView.getDisplayedViewIndex());
                    bundle.putSerializable("OUTLINE", mFlatOutline);
                    intent.putExtra("PALLETBUNDLE", Pallet.sendBundle(bundle));
                }
                activityLauncher.launch(intent);
            }
        });

        int black = ContextCompat.getColor(this, R.color.black1);
        int white = ContextCompat.getColor(this, R.color.white1);

		// Reenstate last state if it was recorded
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		mLayoutEM = prefs.getInt("layoutem"+mDocKey, 14);
		lastPage = prefs.getInt("page" + mDocKey, 0);
		single = prefs.getBoolean("single" + mDocKey, false);
		leftText = prefs.getBoolean("lefttext" + mDocKey, false);
		vertical = prefs.getBoolean("vertical" + mDocKey, false);
		black = prefs.getInt("black" + mDocKey, black);
		white = prefs.getInt("white" + mDocKey, white);

		core.setTintColor(black, white);

        if (!core.isReflowable()) {
            HelpActivity.updateReadme();

            if (leftText) {
                leftText = false;
                toggleTextLeftHighlight();
            }

            if (single) {
                single = false;
                toggleSingleColumnHighlight();
            }

            if (lastPage < core.countPages())
                mDocView.setDisplayedViewIndex(lastPage);

            lastPage = -1;

            if (vertical) {
                vertical = false;
                toggleFlipVerticalHighlight();
            }
        }

		if (savedInstanceState == null || !savedInstanceState.getBoolean("ButtonsHidden", false))
			showButtons();

		if(savedInstanceState != null && savedInstanceState.getBoolean("SearchMode", false))
			searchModeOn();

		// Stick the document view and the buttons overlay into a parent view
		RelativeLayout layout = new RelativeLayout(this);
		// layout.setBackgroundColor(Color.DKGRAY);
		layout.setBackgroundColor(ContextCompat.getColor(this, R.color.toolbar));
		layout.addView(mDocView);
		layout.addView(mButtonsView);
		setContentView(layout);

        watchNavigationBar();
	}

    private void makeColorPopupWindow() {
        List<ColorItem> itemList = new ArrayList<>();

        itemList.add(new ColorItem("Day", ContextCompat.getColor(this, R.color.black1), ContextCompat.getColor(this, R.color.white1)));
        itemList.add(new ColorItem("Night", ContextCompat.getColor(this, R.color.black2), ContextCompat.getColor(this, R.color.white2)));
        itemList.add(new ColorItem("Moon", ContextCompat.getColor(this, R.color.black21), ContextCompat.getColor(this, R.color.white21)));
        itemList.add(new ColorItem("Paper", ContextCompat.getColor(this, R.color.black3), ContextCompat.getColor(this, R.color.white3)));
        itemList.add(new ColorItem("Sepia", ContextCompat.getColor(this, R.color.black4), ContextCompat.getColor(this, R.color.white4)));
        itemList.add(new ColorItem("Twilight", ContextCompat.getColor(this, R.color.black5), ContextCompat.getColor(this, R.color.white5)));
        itemList.add(new ColorItem("Console", ContextCompat.getColor(this, R.color.black6), ContextCompat.getColor(this, R.color.white6)));

        ColorAdapter adapter = new ColorAdapter(this, itemList, new ColorAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ColorItem item =  itemList.get(position);
                if (core.setTintColor(item.black, item.white)) {
                    mDocView.refresh();
                    SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putInt("black"+mDocKey, item.black);
                    edit.putInt("white"+mDocKey, item.white);
                    edit.apply();
                }
                // mColorPopupWindow.dismiss();
            }
        });

        mColorPopupWindow = new ListPopupWindow(this);
        mColorPopupWindow.setAdapter(adapter);
        mColorPopupWindow.setWidth(adapter.getWidth() + 100);
        mColorPopupWindow.setHeight(ListPopupWindow.WRAP_CONTENT);
        mColorPopupWindow.setModal(true);
    }

    private ActivityResultLauncher<Intent> activityLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK) {
                    // There are no request codes
                    Intent data = result.getData();
                    int pagetogo  = data.getExtras().getInt("pagetogo");

                    if (mDocView != null) {
                        mDocView.pushHistory();
                        mDocView.setDisplayedViewIndex(pagetogo - RESULT_FIRST_USER);
                    }
                }
            }
        });


	private void setButtonEnabled(ImageButton button, boolean enabled) {
		button.setEnabled(enabled);
		button.setColorFilter(enabled ? enabledColor : disabledColor);
	}

    @SuppressWarnings("deprecation")
    private void watchNavigationBar() {
        View decorView = getWindow().getDecorView();
        // below android 11 (api30)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // run after onSizeChanged
            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    // if changed by keyboard, do not report
                    if (mKeyboardChanged2) {
                        mKeyboardChanged2 = false;
                        return;
                    }
                    mNavigationBar = (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
                }
            });
        }
        // run before onSizeChanged
        decorView.setOnApplyWindowInsetsListener((v, insets) -> {
            // below android 11 (api30)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                /**
                 * insetTop: 96: statusBar, insetBottom: 0/168: navigationBar
                 */
                // Tool.i("stable");
                // RectI rs = new RectI(insets.getStableInsetLeft(),insets.getStableInsetTop()
                //         ,insets.getStableInsetRight(),insets.getStableInsetBottom());
                // Tool.i(rs);
            }
            else {
                // Tool.i("ime:" + insets.isVisible(WindowInsets.Type.ime()));
                // Insets bar = insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                // Tool.i("inset");
                // RectI rb = new RectI(bar.left, bar.top, bar.right, bar.bottom);
                // Tool.i(rb);

                // if changed by keyboard, do not report
                if (mKeyboardChanged2) {
                    mKeyboardChanged2 = false;
                    return v.onApplyWindowInsets(insets);
                }
                mNavigationBar  = insets.isVisible(WindowInsets.Type.navigationBars());
            }
            /**
             * this return will lock window size so docview won't be disturbed by ime
             * but the immersed navigation bar overlaps with the slider, so do not use it
             */
            // return insets.CONSUMED;
            return v.onApplyWindowInsets(insets);
        });
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mDocKey != null && mDocView != null) {
			if (mDocTitle != null)
				outState.putString("DocTitle", mDocTitle);

			savePrefs();
		}

		if (!mButtonsVisible)
			outState.putBoolean("ButtonsHidden", true);

		if (mTopBarMode == TopBarMode.Search)
			outState.putBoolean("SearchMode", true);

		BookmarkRepository.getInstance().save();
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mSearchTask != null)
			mSearchTask.stop();

		if (mDocKey != null && mDocView != null) {
			savePrefs();
		}
		BookmarkRepository.getInstance().save();
	}

	private void savePrefs() {
		// Store current page in the prefs against the file name,
		// so that we can pick it up each time the file is loaded
		// Other info is needed only for screen-orientation change,
		// so it can go in the bundle
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = prefs.edit();
		edit.putInt("page" + mDocKey, mDocView.getDisplayedViewIndex());
		edit.putBoolean("single" + mDocKey, mSingleColumnHighlight);
		edit.putBoolean("lefttext" + mDocKey, mTextLeftHighlight);
		edit.putBoolean("vertical" + mDocKey, mFlipVerticalHighlight);
		edit.apply();
	}

	@Override
	protected void onDestroy() {
		if (mDocView != null) {
			mDocView.applyToChildren(new ReaderView.ViewMapper() {
				@Override
				public void applyToView(View view) {
					((PageView)view).releaseBitmaps();
				}
			});
		}
		if (core != null)
			core.onDestroy();
		core = null;
		super.onDestroy();
	}

    private void exit() {
        finish();
    }

    private void copy() {
        mDocView.copy();
    }

    private void toggleSingleColumnHighlight() {
        int index;
        mSingleColumnHighlight = !mSingleColumnHighlight;
		// COLOR tint
		mSingleColumnButton.setColorFilter(mSingleColumnHighlight ? highlightColor : highunlightColor);
		// Inform pages of the change.
        core.toggleSingleColumn();
		mDocView.toggleSingleColumn();
		int smax = Math.max(core.countPages()-1,1);
		mPageSliderRes = ((10 + smax - 1)/smax) * 2;
		index = mDocView.getDisplayedViewIndex();
		updatePageNumView(index);
		updatePageSlider(index);
		mFlatOutline = null;
		BookmarkRepository.getInstance().toggleSingleColumn();
    }

    private void toggleTextLeftHighlight() {
		mTextLeftHighlight = !mTextLeftHighlight;
		// COLOR tint
		mTextLeftButton.setColorFilter(mTextLeftHighlight ? highlightColor : highunlightColor);
		// Inform pages of the change.
		core.toggleTextLeft();
		mDocView.toggleTextLeft();
		int index = mDocView.getDisplayedViewIndex();
		updatePageNumView(index);
        updatePageSlider(index);
	}

    private void toggleFlipVerticalHighlight() {
		mFlipVerticalHighlight = !mFlipVerticalHighlight;
		// COLOR tint
		mFlipVerticalButton.setColorFilter(mFlipVerticalHighlight ? highlightColor : highunlightColor);
		// Inform pages of the change.
		mDocView.toggleFlipVertical();
	}

    private void toggleLock() {
		mLockHighlight = !mLockHighlight ;
		// COLOR tint
		mLockButton.setColorFilter(mLockHighlight ? highlightColor : highunlightColor);
		// Inform pages of the change.
		mDocView.toggleLock();
    }

    private void toggleCropMargin() {
		mCropMarginHighlight = !mCropMarginHighlight;
		// COLOR tint
		mCropMarginButton.setColorFilter(mCropMarginHighlight ? highlightColor : highunlightColor);
		// Inform pages of the change.
		core.toggleCropMargin();
		mDocView.toggleCropMargin();
    }

    private void toggleFocus() {
		mFocusHighlight = !mFocusHighlight;
		// COLOR tint
		mFocusButton.setColorFilter(mFocusHighlight ? highlightColor : highunlightColor);
		// Inform pages of the change.
		mDocView.toggleFocus(core.isReflowable());
    }

    private void toggleSmartFocus() {
		mSmartFocusHighlight = !mSmartFocusHighlight;
		// COLOR tint
		mSmartFocusButton.setColorFilter(mSmartFocusHighlight ? highlightColor : highunlightColor);
		// Inform pages of the change.
		mDocView.toggleSmartFocus();
    }

	private void setLinkHighlight(boolean highlight) {
		mLinkHighlight = highlight;
		// LINK_COLOR tint
		mLinkButton.setColorFilter(highlight ? highlightColor : highunlightColor);
		// Inform pages of the change.
		mDocView.setLinksEnabled(highlight);
	}

	private void showButtons() {
		if (core == null)
			return;
		if (!mButtonsVisible) {
			mButtonsVisible = true;
			// Update page number text and slider
			int index = mDocView.getDisplayedViewIndex();
			updatePageNumView(index);
            updatePageSlider(index);
			if (mTopBarMode == TopBarMode.Search) {
                if ("".equals(mSearchText.getText().toString().trim())) {
				    mSearchText.requestFocus();
				    showKeyboard();
                }
			}

			Animation anim = new TranslateAnimation(0, 0, -mTopBarSwitcher.getHeight(), 0);
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mTopBarSwitcher.setVisibility(View.VISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {}
			});
			mTopBarSwitcher.startAnimation(anim);

			// Animate the bottom control container
			final View bottomControl = mBottomControlContainer != null ? mBottomControlContainer : mPageSlider;
			anim = new TranslateAnimation(0, 0, bottomControl.getHeight(), 0);
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					bottomControl.setVisibility(View.VISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
					mPageNumberView.setVisibility(View.VISIBLE);
				}
			});
			bottomControl.startAnimation(anim);
		}
	}

	private void hideButtons() {
		if (true) return; // barra sempre visível — nunca esconder
		if (mButtonsVisible) {
			mButtonsVisible = false;
			hideKeyboard();

			Animation anim = new TranslateAnimation(0, 0, 0, -mTopBarSwitcher.getHeight());
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
					mTopBarSwitcher.setVisibility(View.INVISIBLE);
				}
			});
			mTopBarSwitcher.startAnimation(anim);

			// Animate the bottom control container
			final View bottomControl = mBottomControlContainer != null ? mBottomControlContainer : mPageSlider;
			anim = new TranslateAnimation(0, 0, 0, bottomControl.getHeight());
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mPageNumberView.setVisibility(View.INVISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
					bottomControl.setVisibility(View.INVISIBLE);
				}
			});
			bottomControl.startAnimation(anim);
		}
	}

	private void searchModeOn() {
		if (mTopBarMode != TopBarMode.Search) {
			mTopBarMode = TopBarMode.Search;
			//Focus on EditTextWidget
			mSearchText.requestFocus();
            if ("".equals(mSearchText.getText().toString().trim())) {
			    showKeyboard();
            }
			mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
		}
	}

	private void searchModeOff() {
		if (mTopBarMode == TopBarMode.Search) {
			mTopBarMode = TopBarMode.Main;
			hideKeyboard();
			mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
			SearchTaskResult.set(null);
			// Make the ReaderView act on the change to mSearchTaskResult
			// via overridden onChildSetup method.
			mDocView.resetupChildren();
		}
	}

    private void updatePageSlider(int index) {
        if (core == null)
            return;
		mPageSlider.setMax((core.countPages()-1)*mPageSliderRes);
        if (!mTextLeftHighlight)
			mPageSlider.setProgress(index * mPageSliderRes);
        else
			mPageSlider.setProgress((core.countPages() - 1 - index) * mPageSliderRes);

    }

	private void updatePageNumView(int index) {
        if (core == null)
            return;
		
		// Update floating page number (default UI)
		if (mUseCustomUI) {
			mPageNumberView.setText(String.format(Locale.ROOT, "Page %d of %d", index + 1, core.countPages()));
		} else {
			mPageNumberView.setText(String.format(Locale.ROOT, "%d / %d", index + 1, core.countPages()));
		}
		
		// Update custom navigation bar if in custom UI mode
		if (mUseCustomUI && mPageNavText != null) {
			updateCustomNavBar(index);
		}
	}

	private void makeButtonsView() {
		// Choose layout based on custom UI flag
		int layoutId = mUseCustomUI ? R.layout.document_activity_custom : R.layout.document_activity;
		mButtonsView = getLayoutInflater().inflate(layoutId, null);
		
		mDocNameView = (TextView)mButtonsView.findViewById(R.id.docNameText);
		mDocTypeView = (TextView)mButtonsView.findViewById(R.id.docTypeText);
		mCloseButton = (ImageButton)mButtonsView.findViewById(R.id.closeButton);
		mFloatingCopyButton = (Button) mButtonsView.findViewById(R.id.floatingCopyButton);
		if (mFloatingCopyButton != null) {
			mFloatingCopyButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					copy();
				}
			});
		}
		mPageSlider = (SeekBar)mButtonsView.findViewById(R.id.pageSlider);
		mPageNumberView = (TextView)mButtonsView.findViewById(R.id.pageNumber);
		
		// Custom nav/zoom bar
		mBottomControlContainer = mButtonsView.findViewById(R.id.bottomControlContainer);
		mCustomNavZoomBar = mButtonsView.findViewById(R.id.customNavZoomBar);
		mPagePrevButton = (ImageButton)mButtonsView.findViewById(R.id.pagePrevButton);
		mPageNextButton = (ImageButton)mButtonsView.findViewById(R.id.pageNextButton);
		mPageNavText = (TextView)mButtonsView.findViewById(R.id.pageNavText);
		mZoomInButton = (ImageButton)mButtonsView.findViewById(R.id.zoomInButton);
		mZoomOutButton = (ImageButton)mButtonsView.findViewById(R.id.zoomOutButton);
		mZoomLevelText = (TextView)mButtonsView.findViewById(R.id.zoomLevelText);
		
		// Vote section
		mVoteSection = mButtonsView.findViewById(R.id.voteSection);
		mThumbsUpButton = (ImageButton)mButtonsView.findViewById(R.id.thumbsUpButton);
		mVoteCountView = (TextView)mButtonsView.findViewById(R.id.voteCount);
		
		// Floating message card
		mFloatingMessageCard = mButtonsView.findViewById(R.id.floatingMessageCard);
		mFloatingMessageText = (TextView)mButtonsView.findViewById(R.id.floatingMessageText);
		
		mSearchButton = (ImageButton)mButtonsView.findViewById(R.id.searchButton);
        mCopyButton = (ImageButton)mButtonsView.findViewById(R.id.copyButton);
        mSingleColumnButton = (ImageButton)mButtonsView.findViewById(R.id.singleColumnButton);
        mTextLeftButton = (ImageButton)mButtonsView.findViewById(R.id.textLeftButton);
        mFlipVerticalButton = (ImageButton)mButtonsView.findViewById(R.id.flipVerticalButton);
        mFocusButton = (ImageButton)mButtonsView.findViewById(R.id.focusButton);
        mLockButton = (ImageButton)mButtonsView.findViewById(R.id.lockButton);
        mCropMarginButton = (ImageButton)mButtonsView.findViewById(R.id.cropMarginButton);
        mSmartFocusButton = (ImageButton)mButtonsView.findViewById(R.id.smartFocusButton);
        mColorButton = (ImageButton)mButtonsView.findViewById(R.id.colorButton);
        mShareButton = (ImageButton)mButtonsView.findViewById(R.id.shareButton);
        mHelpButton = (ImageButton)mButtonsView.findViewById(R.id.helpButton);
		mOutlineButton = (ImageButton)mButtonsView.findViewById(R.id.outlineButton);
		mTopBarSwitcher = (ViewAnimator)mButtonsView.findViewById(R.id.switcher);
		mSearchClear = (ImageButton)mButtonsView.findViewById(R.id.searchClear);
		mSearchBack = (ImageButton)mButtonsView.findViewById(R.id.searchBack);
		mSearchFwd = (ImageButton)mButtonsView.findViewById(R.id.searchForward);
		mSearchClose = (ImageButton)mButtonsView.findViewById(R.id.searchClose);
		mSearchText = (EditText)mButtonsView.findViewById(R.id.searchText);
		mLinkButton = (ImageButton)mButtonsView.findViewById(R.id.linkButton);
		mLayoutButton = mButtonsView.findViewById(R.id.layoutButton);
		mOverflowButton = (ImageButton)mButtonsView.findViewById(R.id.overflowButton);
		mTopBarSwitcher.setVisibility(View.INVISIBLE);
		mPageNumberView.setVisibility(View.INVISIBLE);
		mPageSlider.setVisibility(View.INVISIBLE);

		// Configure custom UI elements if enabled
		if (mUseCustomUI) {
			// Set document type
			if (mDocTypeView != null && mDocType != null) {
				mDocTypeView.setText(mDocType);
			}

			// Setup close button
			if (mCloseButton != null) {
				mCloseButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						finish();
					}
				});
			}

			// Setup overflow menu
			setupOverflowMenu();

			// Setup custom navigation/zoom bar
			setupCustomNavZoomBar();

			// Setup vote section
			setupVoteSection();

			// Configure button visibility
			configureButtonVisibility();
		}
	}

	private void configureButtonVisibility() {
		if (mVisibleButtons == null || mVisibleButtons.length == 0) {
			return; // Keep default visibility
		}

		// Create a set for quick lookup
		java.util.Set<String> visibleSet = new java.util.HashSet<>();
		for (String button : mVisibleButtons) {
			visibleSet.add(button);
		}

		// Configure each button's visibility
		setButtonVisibility(mCopyButton, visibleSet.contains(BUTTON_COPY));
		setButtonVisibility(mOutlineButton, visibleSet.contains(BUTTON_OUTLINE));
		setButtonVisibility(mSingleColumnButton, visibleSet.contains(BUTTON_SINGLE_COLUMN));
		setButtonVisibility(mTextLeftButton, visibleSet.contains(BUTTON_TEXT_LEFT));
		setButtonVisibility(mFlipVerticalButton, visibleSet.contains(BUTTON_FLIP_VERTICAL));
		setButtonVisibility(mLockButton, visibleSet.contains(BUTTON_LOCK));
		setButtonVisibility(mCropMarginButton, visibleSet.contains(BUTTON_CROP_MARGIN));
		setButtonVisibility(mFocusButton, visibleSet.contains(BUTTON_FOCUS));
		setButtonVisibility(mSmartFocusButton, visibleSet.contains(BUTTON_SMART_FOCUS));
		setButtonVisibility(mColorButton, visibleSet.contains(BUTTON_COLOR));
		setViewVisibility(mLayoutButton, visibleSet.contains(BUTTON_LAYOUT));
		setButtonVisibility(mLinkButton, visibleSet.contains(BUTTON_LINK));
		setButtonVisibility(mSearchButton, visibleSet.contains(BUTTON_SEARCH));
		setButtonVisibility(mShareButton, visibleSet.contains(BUTTON_SHARE));
		setButtonVisibility(mHelpButton, visibleSet.contains(BUTTON_HELP));
	}

	private void setButtonVisibility(ImageButton button, boolean visible) {
		if (button != null) {
			button.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	private void setViewVisibility(View view, boolean visible) {
		if (view != null) {
			view.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	private void setupOverflowMenu() {
		if (mOverflowButton == null || mCustomMenuItems == null || mCustomMenuItems.length == 0) {
			return;
		}

		// Show overflow button only if there are custom menu items
		mOverflowButton.setVisibility(View.VISIBLE);

		mOverflowButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				PopupMenu popup = new PopupMenu(DocumentActivity.this, mOverflowButton);
				
				// Add custom menu items
				for (int i = 0; i < mCustomMenuItems.length; i++) {
					CustomMenuItem item = mCustomMenuItems[i];
					MenuItem menuItem = popup.getMenu().add(Menu.NONE, i, i, item.getTitle());
					
					// Set icon if provided
					if (item.getIconResId() != 0) {
						menuItem.setIcon(item.getIconResId());
					}
				}

				popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem menuItem) {
						int position = menuItem.getItemId();
						if (position >= 0 && position < mCustomMenuItems.length) {
							CustomMenuItem customItem = mCustomMenuItems[position];
							
							// Check if this is a floating message type
							if (customItem.getType() == CustomMenuItem.TYPE_FLOATING_MESSAGE) {
								// Show floating message
								String message = customItem.getFloatingMessage();
								int style = customItem.getFloatingMessageStyle();
								if (message != null) {
									showFloatingMessage(message, style);
								}
							} else {
								// Regular menu item - call callback
								if (mCallback != null) {
									mCallback.onCustomMenuItemClick(
										DocumentActivity.this,
										customItem.getId(),
										uri,
										mDocTitle
									);
								}
							}
							return true;
						}
						return false;
					}
				});

				popup.show();
			}
		});
	}

	private void setupCustomNavZoomBar() {
		if (mCustomNavZoomBar == null) {
			return;
		}

		// In custom UI mode: hide slider, show custom bar
		if (mPageSlider != null) {
			mPageSlider.setVisibility(View.GONE);
		}
		if (mCustomNavZoomBar != null) {
			mCustomNavZoomBar.setVisibility(View.VISIBLE);
		}

		// Setup page navigation buttons
		if (mPagePrevButton != null) {
			mPagePrevButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mDocView.moveToPrevious();
				}
			});
		}

		if (mPageNextButton != null) {
			mPageNextButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mDocView.moveToNext();
				}
			});
		}

		// Setup zoom buttons
		if (mZoomOutButton != null) {
			mZoomOutButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					zoomOut();
				}
			});
		}

		if (mZoomInButton != null) {
			mZoomInButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					zoomIn();
				}
			});
		}

		// Initial update (will be displayed when showButtons is called)
		updateCustomNavBar(mDocView != null ? mDocView.getDisplayedViewIndex() : 0);
		updateZoomLevel();
	}

	private void updateCustomNavBar(int currentPage) {
		if (mPageNavText == null || core == null) {
			return;
		}

		int totalPages = core.countPages();
		int pageNum = currentPage + 1;

		// Format: "1 / 12" with current page in blue
		String pageText = String.format(Locale.ROOT, "%d / %d", pageNum, totalPages);
		android.text.SpannableString spannableString = new android.text.SpannableString(pageText);
		
		// Color the current page number blue (#1420FF)
		String currentPageStr = String.valueOf(pageNum);
		int startIndex = pageText.indexOf(currentPageStr);
		if (startIndex >= 0) {
			spannableString.setSpan(
				new android.text.style.ForegroundColorSpan(Color.parseColor("#1420FF")),
				startIndex,
				startIndex + currentPageStr.length(),
				android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			);
		}
		mPageNavText.setText(spannableString);

		// Update button states (fade if can't navigate)
		if (mPagePrevButton != null) {
			boolean canGoPrev = pageNum > 1;
			mPagePrevButton.setAlpha(canGoPrev ? 1.0f : 0.3f);
			mPagePrevButton.setEnabled(canGoPrev);
		}

		if (mPageNextButton != null) {
			boolean canGoNext = pageNum < totalPages;
			mPageNextButton.setAlpha(canGoNext ? 1.0f : 0.3f);
			mPageNextButton.setEnabled(canGoNext);
		}
	}

	private void zoomIn() {
		if (mDocView != null) {
			mDocView.scaleChildren(1.25f);
			updateZoomLevel();
		}
	}

	private void zoomOut() {
		if (mDocView != null) {
			mDocView.scaleChildren(0.8f);
			updateZoomLevel();
		}
	}

	private void updateZoomLevel() {
		if (mZoomLevelText == null || mDocView == null) {
			return;
		}

		float scale = mDocView.getScale();
		int percentage = Math.round(scale * 100);
		mZoomLevelText.setText(String.format(Locale.ROOT, "%d%%", percentage));
	}

	private void setupVoteSection() {
		if (mVoteSection == null || !mShowVote) {
			return;
		}

		// Show vote section
		mVoteSection.setVisibility(View.VISIBLE);
		
		// Set initial vote count and state
		mVoteCountView.setText(String.valueOf(mVoteCount));
		updateThumbsUpButton();
		
		// Setup click listener
		if (mThumbsUpButton != null) {
			mThumbsUpButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					toggleVote();
				}
			});
		}
	}

	private void toggleVote() {
		// Toggle vote state
		mIsVoted = !mIsVoted;
		
		// Update count
		if (mIsVoted) {
			mVoteCount++;
		} else {
			mVoteCount = Math.max(0, mVoteCount - 1);
		}
		
		// Update UI
		mVoteCountView.setText(String.valueOf(mVoteCount));
		updateThumbsUpButton();
		
		// Notify callback
		if (mCallback != null) {
			mCallback.onVoteChanged(this, uri, mDocTitle, mIsVoted, mVoteCount);
		}
	}

	private void updateThumbsUpButton() {
		if (mThumbsUpButton == null) {
			return;
		}
		
		// Change color/alpha based on voted state
		if (mIsVoted) {
			mThumbsUpButton.setAlpha(1.0f);
			mThumbsUpButton.setColorFilter(Color.parseColor("#1420FF"));
		} else {
			mThumbsUpButton.setAlpha(0.6f);
			mThumbsUpButton.setColorFilter(null);
		}
	}

	private void showFloatingMessage(String message, int style) {
		if (mFloatingMessageCard == null || mFloatingMessageText == null) {
			return;
		}

		// Set text with style
		mFloatingMessageText.setText(message);
		mFloatingMessageText.setTypeface(null, style);
		
		// Show the card
		mFloatingMessageCard.setVisibility(View.VISIBLE);
		
		// Setup dismiss on touch
		setupFloatingMessageDismiss();
	}

	private void setupFloatingMessageDismiss() {
		// Dismiss when clicking on the card itself
		if (mFloatingMessageCard != null) {
			mFloatingMessageCard.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					hideFloatingMessage();
				}
			});
		}
		
		// Dismiss when tapping document
		if (mDocView != null) {
			mDocView.setOnTouchListener(new View.OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						if (mFloatingMessageCard != null && mFloatingMessageCard.getVisibility() == View.VISIBLE) {
							hideFloatingMessage();
						}
					}
					return false; // Let the event continue
				}
			});
		}
	}

	private void hideFloatingMessage() {
		if (mFloatingMessageCard != null) {
			mFloatingMessageCard.setVisibility(View.GONE);
		}
		
		// Remove touch listener from docView
		if (mDocView != null) {
			mDocView.setOnTouchListener(null);
		}
	}

	private void showKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.showSoftInput(mSearchText, 0, rr);
        }
	}

	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0, rr);
        }
	}

    private ResultReceiver rr = new ResultReceiver(mHandler) {
        @Override
        protected void onReceiveResult(int code, Bundle data) {
            if (code == InputMethodManager.RESULT_SHOWN
                    || code == InputMethodManager.RESULT_HIDDEN) {
                if (!mNavigationBar) {
                    mKeyboardChanged = true;
                    mKeyboardChanged2 = true;
                    mKeyboardChanged3 = (code == InputMethodManager.RESULT_SHOWN);
                }
            }
        }
    };

    @SuppressWarnings("deprecation")
    public void updateTopBar(Integer w) {
        if (w == null) {

            // below android 11 (api30)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();
                Insets insets = windowMetrics.getWindowInsets()
                        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                w = windowMetrics.getBounds().width() - insets.left - insets.right;
            } else {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		        w = (int)displayMetrics.widthPixels;
            }
        }
        int BUTTON_WIDTH = 160;
        // topbar button count
        int cbut = 9;
        if (mCopyButton.getVisibility() == View.VISIBLE) cbut++;
        if (mSingleColumnButton.getVisibility() == View.VISIBLE) cbut++;
        if (mTextLeftButton.getVisibility() == View.VISIBLE) cbut++;
        if (mSmartFocusButton.getVisibility() == View.VISIBLE) cbut++;
        if (mLayoutButton.getVisibility() == View.VISIBLE) cbut++;
        if (mOutlineButton.getVisibility() == View.VISIBLE) cbut++;
        int tw = w - BUTTON_WIDTH * cbut;
        int titlebytelen = mDocNameView.getText().toString().getBytes().length;
        int titlewidth = titlebytelen * 32;
        int minwidth = Math.min(titlewidth, 360);
        tw = Math.max(tw, minwidth);
        mDocNameView.setWidth(tw);
    }

    public void showCopyButton(int vis) {
        if (mCopyButton.getVisibility() != vis) {
            mCopyButton.setVisibility(vis);
            updateTopBar(null);
        }
    }

	public void showFloatingCopyButton(float x, float y) {
		if (mFloatingCopyButton == null) return;
		mFloatingCopyButton.setVisibility(View.VISIBLE);
		FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mFloatingCopyButton.getLayoutParams();
		int topBarHeight = (mTopBarSwitcher != null) ? mTopBarSwitcher.getHeight() : 0;
		lp.leftMargin = Math.max(0, (int) x - 80);
		lp.topMargin = Math.max(topBarHeight + 8, (int) y - 160);
		mFloatingCopyButton.setLayoutParams(lp);
	}

	public void hideFloatingCopyButton() {
		if (mFloatingCopyButton != null) mFloatingCopyButton.setVisibility(View.GONE);
	}

    public void showSingleColumnButton(int vis) {
        if (mOrientationChanged) {
            mOrientationChanged = false;
        }
        else if (core.isReflowable()) {
            return;
        }
        else if (mSingleColumnButton.getVisibility() != vis) {
            mSingleColumnButton.setVisibility(vis);
            updateTopBar(null);
        }
    }

	public void createBookmark() {
		if (true) return; // bookmark por toque longo desativado
		AlertDialog alert = mAlertBuilder.create();
		alert.setTitle(R.string.create_bookmark);
		alert.setMessage(getString(R.string.create_bookmark_message));
		alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes),
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					BookmarkRepository.getInstance().create(mDocView.getDisplayedViewIndex());
				}
			}
		);
		alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no),
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			}
		);
		alert.show();
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation != mOrientation) {
            mOrientation = newConfig.orientation;
            mOrientationChanged = true;
            searchModeOff();
        }
    }

	private void search(int direction) {
		hideKeyboard();
		int displayPage = mDocView.getDisplayedViewIndex();
		SearchTaskResult r = SearchTaskResult.get();
		int searchPage = r != null ? r.pageNumber : -1;
		mSearchTask.go(mSearchText.getText().toString().trim(), direction, displayPage, searchPage);
	}

	@Override
	public boolean onSearchRequested() {
		if (mButtonsVisible && mTopBarMode == TopBarMode.Search) {
			hideButtons();
		} else {
			showButtons();
			searchModeOn();
		}
		return super.onSearchRequested();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mButtonsVisible && mTopBarMode != TopBarMode.Search) {
			hideButtons();
		} else {
			showButtons();
			searchModeOff();
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		if (mDocView == null || (mDocView != null && !mDocView.popHistory())) {
			super.onBackPressed();
			if (mReturnToLibraryActivity) {
				Intent intent = getPackageManager().getLaunchIntentForPackage(getComponentName().getPackageName());
				startActivity(intent);
			}
		}
	}
}
