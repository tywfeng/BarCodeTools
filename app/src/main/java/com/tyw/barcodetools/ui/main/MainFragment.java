package com.tyw.barcodetools.ui.main;

import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProviders;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tyw.barcodetools.R;
import com.tyw.barcodetools.utils.DebugLog;
import com.yzq.zxinglibrary.android.CaptureActivity;
import com.yzq.zxinglibrary.common.Constant;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainFragment extends Fragment {

    // 选项首选项
    private static final String PREF_OPTIONS = "Pref_OPTIONS";
    // 配置文件路径
    private static final String PREF_OPTIONS_CONFIG_PATH = "Pref_ConfigPath";

    @BindView(R.id.btn_change_file)
    Button mBtnSelectXLS;                // 路径选择xls
    @BindView(R.id.tv_path)
    TextView mTvXLSPath;             // 路径
    @BindView(R.id.btn_scan)
    Button mBtnScan;                // 扫描
    @BindView(R.id.tv_result)
    TextView mTvResult;             // 结果
    @BindView(R.id.et_input)
    EditText mEtInput;              // 输入检索
    @BindView(R.id.btn_input_search)
    Button mBtnSearch;              // 输入检索

    private MainViewModel mViewModel;

    private static final int REQUESTCODE_CHANGE_CONFIGPATH = 1;         // 选择xls文件
    private static final int REQUESTCODE_CAPTURE = 2;                   // 扫码

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment, container, false);
        ButterKnife.bind(this, view);

        String strPath = getDocPath(getActivity());
        mTvXLSPath.setText(strPath);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(MainViewModel.class);
        // TODO: Use the ViewModel
    }


    @OnClick(R.id.btn_scan)
    public void onScan() {
        Intent intent = new Intent(getActivity(),CaptureActivity.class);
        startActivityForResult(intent, REQUESTCODE_CAPTURE);
    }
    @OnClick(R.id.btn_input_search)
    void onSearch() {
        String input = mEtInput.getText().toString();

        if (input == null || input.trim().equals("")) {
            Toast.makeText(getActivity(), "无效输入", Toast.LENGTH_SHORT).show();
            return;
        }

        onScanResult(input);
    }

    private void onScanResult(String result) {
        if (mTvXLSPath == null || mTvXLSPath.getText().toString().trim().equals("")) {
            Toast.makeText(getActivity(), "选择一个XLS文件", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Workbook workBook = Workbook.getWorkbook(new File(mTvXLSPath.getText().toString()));
            if (workBook == null) {
                Toast.makeText(getActivity(), "无法加载XLS文件", Toast.LENGTH_SHORT).show();
                return;
            }
            Sheet sheet = workBook.getSheet(0);
            if (sheet == null) {
                Toast.makeText(getActivity(), "XLS内无sheet", Toast.LENGTH_SHORT).show();
                return;
            }
            Cell findCell = sheet.findCell(result);
            if (findCell == null) {
                String strHint = "未找到:" + result;
                Toast.makeText(getActivity(), strHint, Toast.LENGTH_SHORT).show();
                mTvResult.setText(strHint);
                return;
            }
            Cell[] firstCells = sheet.getRow(0);
            if (firstCells == null || firstCells.length < 2) {
                Toast.makeText(getActivity(), "请在第一行设置标题（至少2列）", Toast.LENGTH_SHORT).show();
                return;
            }
            // 遍历读取
            StringBuilder strBuilder = new StringBuilder(getString(R.string.result_title,result));
            for (int i = 0; i < firstCells.length; ++i) {
                Cell data = sheet.getCell(i, findCell.getRow());
                if (data != null) {
                    strBuilder.append(getString(R.string.result_item,firstCells[i].getContents(),data.getContents()));
                    //strBuilder.append(firstCells[i].getContents()).append(":").append(data.getContents()).append(System.getProperty("line.separator"));
                }
            }
            mTvResult.setText(HtmlCompat.fromHtml(strBuilder.toString(),Html.FROM_HTML_MODE_LEGACY));
        } catch (IOException e) {
            e.printStackTrace();
            DebugLog.e(e.getMessage());
        } catch (BiffException e) {
            e.printStackTrace();
            DebugLog.e(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            DebugLog.e(e.getMessage());
        }
    }

    @OnClick(R.id.btn_change_file)
    void onChangePath() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.ms-excel");//无类型限制
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUESTCODE_CHANGE_CONFIGPATH);
    }

    // 提取路径
    public static String getDocPath(Context context) {
        String strSDPath = getSDPath();
        if (strSDPath == null) return null;
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_OPTIONS
                , Context.MODE_PRIVATE);
        String strDefault = "";
        return sharedPref.getString(PREF_OPTIONS_CONFIG_PATH, strDefault);
    }

    // 保存路径
    public static void SaveDocPath(Context context, String _path) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_OPTIONS
                , Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PREF_OPTIONS_CONFIG_PATH, _path);
        editor.commit();
    }

    public static String getSDPath() {
        File sdDir = null;
        //判断sd卡是否存在
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            //获取根目录 Environment.getExternalStorageDirectory().getAbsolutePath()
            sdDir = Environment.getExternalStorageDirectory();
            if (sdDir != null) return sdDir.getAbsolutePath();
        }
        return null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        try {
            // 扫码结果
            if (requestCode == REQUESTCODE_CAPTURE && resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    String scanResult = data.getStringExtra(Constant.CODED_CONTENT);

                    if (scanResult == null || scanResult.trim().equals("")) {
                        Toast.makeText(getActivity(), "无效的扫描结果", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    onScanResult(scanResult);
                }
            } else if (requestCode == REQUESTCODE_CHANGE_CONFIGPATH && resultCode == Activity.RESULT_OK) {
                String path;
                if (data == null) return;
                Uri uri = data.getData();
                if ("file".equalsIgnoreCase(uri.getScheme())) {//使用第三方应用打开
                    path = uri.getPath();
                    mTvXLSPath.setText(path);
                    Toast.makeText(getContext(), "GetConfigFile From TheThirdParty Tool:" + path, Toast.LENGTH_SHORT).show();
                    //tv.setText(path);
                    //Toast.makeText(this,path+"11111",Toast.LENGTH_SHORT).show();
                    //return;
                } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
                    {
                        path = getFilePathForN(getActivity(), uri);
                    }else {
                        path = getPath(getActivity(), uri);
                    }
                    mTvXLSPath.setText(path);
                    Toast.makeText(getContext(), "Android > 4.4:" + path, Toast.LENGTH_SHORT).show();
                } else {
                    //4.4以下下系统调用方法
                    path = getRealPathFromURI(uri);
                    mTvXLSPath.setText(path);
                    Toast.makeText(getContext(), "Android <= 4.4:" + path, Toast.LENGTH_SHORT).show();
                }
                if (path != null) {
                    //save
                    SaveDocPath(getActivity(), path);
                }
                return;
            }
        }catch (Exception e)
        {
            DebugLog.e(e.getMessage());
            e.printStackTrace();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private static String getFilePathForN( Context context,Uri uri) {
        Uri returnUri = uri;
        Cursor returnCursor = context.getContentResolver().query(returnUri, null, null, null, null);
        /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        String size = (Long.toString(returnCursor.getLong(sizeIndex)));
        File file = new File(context.getFilesDir(), name);
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            int maxBufferSize = 1 * 1024 * 1024;
            int bytesAvailable = inputStream.available();

            //int bufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);

            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            DebugLog.e(  e.getMessage());
        }
        return file.getPath();
    }
    /**
     * 专为Android4.4设计的从Uri获取文件绝对路径，以前的方法已不好使
     */
    @SuppressLint("NewApi")
    public String getPath(final Context context, final Uri uri) {


        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;


        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];


                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {


                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));


                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];


                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }


                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};


                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getActivity().getContentResolver().query(contentUri, proj, null, null, null);
        if (null != cursor && cursor.moveToFirst()) {
            ;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
            cursor.close();
        }
        return res;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {


        Cursor cursor = null;
        final String column = MediaStore.MediaColumns.DATA;
        final String[] projection = {column};


        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }



    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
