package com.fmsh.einkesl.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.fmsh.base.ui.BaseActivity;
import com.fmsh.base.utils.HintDialog;
import com.fmsh.base.utils.UIUtils;
import com.fmsh.einkesl.App;
import com.fmsh.einkesl.R;
import com.fmsh.einkesl.tools.image.BmpUtils;
import com.fmsh.einkesl.tools.image.ImageUtils;
import com.fmsh.einkesl.utils.IUtils;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.entity.LocalMedia;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * @author wuyajiang
 * @date 2021/5/28
 */
public class TextGenerateBmpActivity extends BaseActivity {
    @BindView(R.id.et_text1)
    EditText etText1;
    @BindView(R.id.imgpic)
    ImageView imgpic;
    @BindView(R.id.topbar)
    QMUITopBarLayout topbar;
    @BindView(R.id.et_text2)
    EditText etText2;
    @BindView(R.id.et_text3)
    EditText etText3;
    @BindView(R.id.et_text4)
    EditText etText4;
    @BindView(R.id.spinnerTexFontName)
    Spinner spinnerTexFontName;
    @BindView(R.id.checkboxBold)
    CheckBox checkboxBold;
    @BindView(R.id.checkboxItalics)
    CheckBox checkboxItalics;
    @BindView(R.id.checkboxUnderLine)
    CheckBox checkboxUnderLine;
    @BindView(R.id.spinnerTextColor)
    Spinner spinnerTextColor;
    @BindView(R.id.spinnerTextSize)
    Spinner spinnerTextSize;
    @BindView(R.id.btn_import)
    Button btnImport;
    private int mWidth = App.getDeviceInfo().getWidth();
    private int mHeight = App.getDeviceInfo().getHeight();
    String[] m_FontNameitems = new String[]{"Song", "Colorful Chinese", "Simkai"};
    String[] m_FontNameitemsEx = new String[]{"STSONG.TTF", "STCAIYUN.TTF", "simkai.ttf"};

    final int textObjCount = 4;
    String[] strTextObj = new String[textObjCount];
    private int[] fontcolor = new int[textObjCount];
    private boolean m_blnBold[] = new boolean[textObjCount];
    private boolean m_underline[] = new boolean[textObjCount];
    private boolean m_Itali[] = new boolean[textObjCount];
    private int[] m_textSize = new int[textObjCount];
    private String[] m_strFontName = new String[textObjCount];
    int m_nCurrentTextObjIndex = 0;
    private BitmapFactory.Options mOptions;
    private String mCurrentRealPath;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_text_generate_bmp;
    }

    private void InitTextObj() {
        for (int i = 0; i < textObjCount; i++) {
            m_strFontName[i] = "STSONG.TTF";
            fontcolor[i] = Color.BLACK;
            m_textSize[i] = 12;
            m_blnBold[i] = false;
            m_underline[i] = false;
            m_Itali[i] = false;
        }
    }

    @Override
    protected void initView() {
        setTitle(UIUtils.getString(mContext, R.string.string_res_40));
        setBackImage();
        mTopBar.addRightImageButton(R.mipmap.save, 0x11).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BmpUtils.saveBmp(bitmap);
                Bundle bundle = new Bundle();
                bundle.putString("TextGenerateBmpActivity", BmpUtils.getImagePath());
                startActivity(bundle, RefreshScreenActivity.class);
                finish();

            }
        });
        InitTextObj();
        if (App.getDeviceInfo().getColorCount() == 2) {
            spinnerTextColor.setEnabled(false);
        }
        //文字的颜色
        String[] items = new String[]{"Black", "Red"};
        ArrayAdapter<String> aa = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
        spinnerTextColor.setAdapter(aa);
        spinnerTextColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    fontcolor[m_nCurrentTextObjIndex] = Color.BLACK;
                } else {
                    fontcolor[m_nCurrentTextObjIndex] = Color.RED;
                }
                DrawText();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        //字体名称
        aa = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, m_FontNameitems);
        spinnerTexFontName.setAdapter(aa);
        spinnerTexFontName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                m_strFontName[m_nCurrentTextObjIndex] = m_FontNameitemsEx[position];
                DrawText();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        //文字大小
        items = new String[20];
        for (int i = 0; i < 20; i++) {
            items[i] = String.format("%d", i + 15);
        }
        aa = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
        spinnerTextSize.setAdapter(aa);
        spinnerTextSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                m_textSize[m_nCurrentTextObjIndex] = position + 15;
                DrawText();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        checkboxBold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetFontStyle();
                DrawText();
            }
        });
        checkboxItalics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetFontStyle();
                DrawText();
            }
        });
        checkboxUnderLine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetFontStyle();
                DrawText();
            }
        });
    }

    private void GetFontStyle() {
        m_blnBold[m_nCurrentTextObjIndex] = checkboxBold.isChecked();
        m_underline[m_nCurrentTextObjIndex] = checkboxUnderLine.isChecked();
        m_Itali[m_nCurrentTextObjIndex] = checkboxItalics.isChecked();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initData() {
        etText1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                m_nCurrentTextObjIndex = 0;

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                m_nCurrentTextObjIndex = 0;
            }

            @Override
            public void afterTextChanged(Editable s) {
                m_nCurrentTextObjIndex = 0;
                strTextObj[0] = etText1.getText().toString();
                DrawText();
            }
        });
        etText2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                m_nCurrentTextObjIndex = 1;

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                m_nCurrentTextObjIndex = 1;
            }

            @Override
            public void afterTextChanged(Editable s) {
                m_nCurrentTextObjIndex = 1;
                strTextObj[1] = etText2.getText().toString();
                DrawText();
            }
        });
        etText3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                m_nCurrentTextObjIndex = 2;

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                m_nCurrentTextObjIndex = 2;
            }

            @Override
            public void afterTextChanged(Editable s) {
                m_nCurrentTextObjIndex = 2;
                strTextObj[2] = etText3.getText().toString();
                DrawText();
            }
        });
        etText4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                m_nCurrentTextObjIndex = 3;

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                m_nCurrentTextObjIndex = 3;
            }

            @Override
            public void afterTextChanged(Editable s) {
                m_nCurrentTextObjIndex = 3;
                strTextObj[3] = etText4.getText().toString();
                DrawText();
            }
        });
        etText1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                m_nCurrentTextObjIndex = 0;
                checkboxBold.setChecked(m_blnBold[0]);
                checkboxItalics.setChecked(m_Itali[0]);
                checkboxUnderLine.setChecked(m_underline[0]);
            }
        });
        etText2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                m_nCurrentTextObjIndex = 1;
                checkboxBold.setChecked(m_blnBold[1]);
                checkboxItalics.setChecked(m_Itali[1]);
                checkboxUnderLine.setChecked(m_underline[1]);
            }
        });
        etText3.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                m_nCurrentTextObjIndex = 2;
                checkboxBold.setChecked(m_blnBold[2]);
                checkboxItalics.setChecked(m_Itali[2]);
                checkboxUnderLine.setChecked(m_underline[2]);
            }
        });
        etText4.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                m_nCurrentTextObjIndex = 3;
                checkboxBold.setChecked(m_blnBold[3]);
                checkboxItalics.setChecked(m_Itali[3]);
                checkboxUnderLine.setChecked(m_underline[3]);
            }
        });
        imgpic.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int controlw = imgpic.getWidth();
                int controlh = imgpic.getHeight();
                float scaleWidth = ((float) mWidth / controlw);
                float scaleHeight = ((float) mHeight / controlh);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 获取手按下时的坐标

                        m_posX[m_nCurrentTextObjIndex] = (int) (event.getX());
                        m_posY[m_nCurrentTextObjIndex] = (int) (event.getY());


                        m_posX[m_nCurrentTextObjIndex] = (int) (m_posX[m_nCurrentTextObjIndex] * scaleWidth);
                        m_posY[m_nCurrentTextObjIndex] = (int) (m_posY[m_nCurrentTextObjIndex] * scaleHeight);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // 获取手移动后的坐标
                        m_posX[m_nCurrentTextObjIndex] = (int) event.getX();
                        m_posY[m_nCurrentTextObjIndex] = (int) event.getY();
                        //m_x = m_posX;
                        // m_y = m_posY;

                        m_posX[m_nCurrentTextObjIndex] = (int) (m_posX[m_nCurrentTextObjIndex] * scaleWidth);
                        m_posY[m_nCurrentTextObjIndex] = (int) (m_posY[m_nCurrentTextObjIndex] * scaleHeight);
                        DrawText();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        mOptions = new BitmapFactory.Options();

        //默认值为false，如果设置成true，那么在解码的时候就不会返回bitmap，即bitmap = null。
        mOptions.inJustDecodeBounds = false;
        //可以复用之前用过的bitmap
        mOptions.inBitmap = null;
        //是该bitmap缓存是否可变，如果设置为true，将可被inBitmap复用
        mOptions.inMutable = true;
        bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        canvasTemp = new Canvas(bitmap);
        imgpic.setImageBitmap(bitmap);
    }

    int[] m_posX = new int[textObjCount];
    int[] m_posY = new int[textObjCount];
    Bitmap bitmap = null;
    Canvas canvasTemp = null;
    Paint m_paint = new Paint();

    public void DrawText() {
        if (mCurrentRealPath == null) {
            canvasTemp.drawColor(Color.WHITE);
        } else {
            bitmap = BitmapFactory.decodeFile(mCurrentRealPath, mOptions);
            canvasTemp = new Canvas(bitmap);
        }

        for (int i = 0; i < textObjCount; i++) {
            if (strTextObj[i] == null) {
                continue;
            }
            Typeface font = Typeface.createFromAsset(getAssets(), "fonts/" + m_strFontName[i]);
            if (m_blnBold[i] && m_Itali[i]) {
                font = Typeface.create(font, Typeface.BOLD_ITALIC);
            } else if (m_blnBold[i] && (!m_Itali[i])) {
                font = Typeface.create(font, Typeface.BOLD);
            } else if (!m_blnBold[i] && (m_Itali[i])) {
                font = Typeface.create(font, Typeface.ITALIC);
            } else {
                font = Typeface.create(font, Typeface.NORMAL);
            }

            m_paint.setColor(fontcolor[i]);

            m_paint.setFakeBoldText(m_blnBold[i]);
            m_paint.setUnderlineText(m_underline[i]);
            m_paint.setTypeface(font);


            m_paint.setTextSize(m_textSize[i]);

            String[] strLine = strTextObj[i].split("\n");
            Rect rect = new Rect();
            m_paint.getTextBounds("kk", 0, 1, rect);

            if (null != strLine) {
                for (int j = 0; j < strLine.length; j++) {
                    canvasTemp.drawText(strLine[j], m_posX[i] - rect.width(), m_posY[i] + j * rect.height() + rect.height() + rect.height(), m_paint);

                }
            } else {
                canvasTemp.drawText(etText1.getText().toString(), m_posX[i] - rect.width(), m_posY[i], m_paint);

            }
        }

        imgpic.setImageBitmap(bitmap);

    }

    @OnClick(R.id.btn_import)
    public void onClick() {
        IUtils.selectPicture(TextGenerateBmpActivity.this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case PictureConfig.CHOOSE_REQUEST:
                    List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
                    LocalMedia localMedia = selectList.get(0);
                    String fileName = localMedia.getFileName();
                    String realPath = localMedia.getRealPath();
                    if (realPath == null) {
                        realPath = localMedia.getPath();
                    }
                    boolean bmpFormat = BmpUtils.GetBmpFormat(realPath, App.getDeviceInfo());
                    if (bmpFormat) {
                        mCurrentRealPath = realPath;
                        bitmap = BitmapFactory.decodeFile(realPath, mOptions);
                        canvasTemp = new Canvas(bitmap);
                        imgpic.setImageBitmap(bitmap);
                    } else {
                        HintDialog.faileDialog(mContext, UIUtils.getString(R.string.bmp_error));
                    }
                    break;
                default:
                    break;
            }
        }

    }
}
