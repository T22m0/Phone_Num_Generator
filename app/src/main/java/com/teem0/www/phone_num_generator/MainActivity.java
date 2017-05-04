package com.teem0.www.phone_num_generator;

import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    //전화번호부를 불러올수있는 리스트임
    ArrayList<ContentProviderOperation> op = new ArrayList<>();
    //개수 저장 변수
    private int count=0;
    //전화번호부 저장 변수
    private String prefix=null,
                    group=null;
    //GUI와의 연결고리
    private EditText count_in,
                      prefix_in,
                      group_in;
    private Button generate;

    //Korean phone number pool
    final static int MAX_NUM = 1100000000;
    final static int MIN_NUM = 1019999999;
    //pregressbar를 위한 Async 클래스
    //여기서 instantiate하면 에러발생해서 꺼짐.. 왜인지는 잘모름아직
    createContact task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //권한 체크
        checkPermission();

        //Gui 연결
        count_in = (EditText) findViewById(R.id.count_in);
        prefix_in= (EditText) findViewById(R.id.prefix_in);
        group_in = (EditText) findViewById(R.id.group_in);
        generate = (Button) findViewById(R.id.generate);
        //버튼리스너 달아줌
        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //버튼클릭시 일단 값을 읽어옴
                    //exception을 감싸줘야 아무것도 없는 경우 예외처리를 당하지않는다
                    count = Integer.parseInt(count_in.getText().toString());
                    prefix = prefix_in.getText().toString();
                    group = group_in.getText().toString();
                    //count가 0보다커야 클래스 생성후 실행
                    if (count > 0) {
                        new createContact(MainActivity.this).execute(count);
                    }else{
                        Toast.makeText(getApplicationContext(), "0보다 큰숫자를 입력해주세요..", Toast.LENGTH_SHORT).show();
                    }
                }catch(Exception e){
                    Toast.makeText(getApplicationContext(), "숫자 입력해주세요..", Toast.LENGTH_SHORT).show();
                }
                //0을 입력하면 에러 발산!!
            }
        });
    }

    private class createContact extends AsyncTask<Integer,//excute()실행시 넘겨줄 데이터타입
                                                    String,//진행정보 데이터 타입 publishProgress(), onProgressUpdate()의 인수
                                                    Integer>{//doInBackground() 종료시 리턴될 데이터 타입 onPostExecute()의 인수

        private ProgressDialog asyncDialog;
        private Context mContext;
        //constructor - context를 받아준다
        private createContact(Context context) {
            mContext = context;
        }
        //background실행하기전에 실행되는 세팅
        @Override
        protected void onPreExecute() {
            asyncDialog = new ProgressDialog(mContext);
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            asyncDialog.setMessage("생성중...");
            asyncDialog.show();
            super.onPreExecute();
        }

        //doInBackground 함수는 excute() 실행시  실행됨
        @Override
        protected Integer doInBackground(Integer... arg) {
            Random r = new Random();
            //arg에서 갯수를 받아 ㅓ장한다
            final int taskCnt = arg[0];
            //넘겨받은 작업개수를 ProgressDialog의 맥스값으로 세팅하기 위해 publishProgress()로 데이터를 넘겨준다.
            //publishProgress()로 넘기면 onProgressUpdate()함수가 실행된다.
            publishProgress("max", Integer.toString(taskCnt));

            //만약 빈칸으로 냅두면 그냥 기본값을줌
            if (prefix == null || prefix.equals("")) prefix = "닝겐";
            if (group == null || group.equals("")) group = "마케팅";

            // 전화번호 숫자 시작
            int startnumber = MIN_NUM;

            // 최대 9999 9999 까지 번호까지 돌아가면서 랜덤으로 뽑기위해
            // 랜덤인수로 정해줄 간격이당
            int numberpool = (MAX_NUM - MIN_NUM) / taskCnt;

            int nextnum;
            for (int i = 0; i < taskCnt; i++) {
                nextnum = r.nextInt(numberpool);
                addContact("0" + String.valueOf(startnumber + nextnum), i);
                startnumber += nextnum;
                try {
                    getContentResolver().applyBatch(ContactsContract.AUTHORITY, op);
                    op.clear();
                    //작업 진행 마다 진행률을 갱신하기 위해 진행된 개수와 설명을 publishProgress() 로 넘겨줌.
                    publishProgress("progress", Integer.toString(i), "작업 번호 " + Integer.toString(i) + "번 수행중");
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "뭔가 에러가 생김 삭삭에게 문의하세요..", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            //작업이 끝나고 작업된 개수를 리턴 . onPostExecute()함수의 인수가 됨
            return taskCnt;
        }
        //onProgressUpdate() 함수는 publishProgress() 함수로 넘겨준 데이터들을 받아옴
        @Override
        protected void onProgressUpdate(String... progress) {
            if (progress[0].equals("progress")) {
                asyncDialog.setProgress(Integer.parseInt(progress[1]));
                asyncDialog.setMessage(progress[2]);
            }
            else if (progress[0].equals("max")) {
                asyncDialog.setMax(Integer.parseInt(progress[1]));
            }
        }
        @Override
        protected void onPostExecute(Integer result) {
            asyncDialog.dismiss();
            Toast.makeText(getApplicationContext(), String.valueOf(result)+"개 전번 생성완료!", Toast.LENGTH_SHORT).show();
            super.onPostExecute(result);
        }
    }


    public void checkPermission() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(getApplicationContext(), "전화번호 생성을 위해서는 권한이 필요합니다!! 허용해주셔요", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_CONTACTS},123);
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.WRITE_CONTACTS)){
                Toast.makeText(getApplicationContext(), "주소록권한을 허용해주셔야 번호를등록하죠...ㅜ",Toast.LENGTH_LONG).show();
                finish();
            }else{
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_CONTACTS},123);
            }
        }
    }

    public void addContact(String phone, int idx) {

        op.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());
        //add name
        op.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, prefix + "_" +String.valueOf(idx+1))
                .build());
        //add phone number
        op.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());
        //add organization
        op.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, group)
                .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                .build());
    }

}
