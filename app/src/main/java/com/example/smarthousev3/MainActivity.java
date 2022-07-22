package com.example.smarthousev3;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity
{
    String SERVER1_IP = "192.168.1.84";
    int SERVER1_PORT = 80;
    String SERVER2_IP = "192.168.1.89";
    int SERVER2_PORT = 80;
    Connection connection1;
    Connection connection2;

    private TabHost tabHost;

    // view на первой вкладке
    private ProgressBar progressBarServer1;
    private WebView webViewWater;
    private TextView textViewProgress;
    private RadioButton radioButtonHome;
    private RadioButton radioButtonStreet;
    private Switch switchPump;
    private TextView textViewPumpTimeTitle;
    private TextView textViewPumpTimeNum;
    private Button refreshButton;

    // view на второй вкладке
    private ProgressBar progressBarServer2;
    private Button gLAviaryButton;
    private Switch switchCHLight;

    // переменные, проверяющие прошла ли загрузка server1:
    private boolean isProgressBarWater = false;
    private boolean isRadioButtonMode = false;
    private boolean isSwitchPump = false;
    private boolean isTextViewPumpTimeNum = false;

    // переменные, проверяющие прошла ли загрузка server2:
    private boolean isSwitchCHLight = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getSupportActionBar().hide();

        tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec("tag1");
        tabSpec.setContent(R.id.linearLayout);
        tabSpec.setIndicator("Бак");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("tag2");
        tabSpec.setContent(R.id.linearLayout2);
        tabSpec.setIndicator("Птичник");
        tabHost.addTab(tabSpec);

        tabHost.setCurrentTab(0);

        refreshButton = findViewById(R.id.buttonRefresh);

        // view на первой вкладке
        progressBarServer1 = findViewById(R.id.progressBarServer1);
        webViewWater = findViewById(R.id.webViewWater);
        textViewProgress = findViewById(R.id.text_view_progress);
        radioButtonHome = findViewById(R.id.radioButtonModeHome);
        radioButtonStreet = findViewById(R.id.radioButtonModeStreet);
        textViewPumpTimeTitle = findViewById(R.id.textViewPumpTimeTitle);
        textViewPumpTimeNum = findViewById(R.id.textViewPumpTimeNum);
        textViewPumpTimeTitle.setText("Насос работал:");
        textViewPumpTimeNum.setText("0 минут.");
        radioButtonHome.setOnClickListener(radioButtonClickListener);
        radioButtonStreet.setOnClickListener(radioButtonClickListener);
        switchPump = findViewById(R.id.switchPump);

        // Делаем невидимым все на первой вкладке, кроме загрузки
        webViewWater.setVisibility(View.INVISIBLE);
        webViewWater.getSettings().setJavaScriptEnabled(true);
        webViewWater.loadUrl("file:///android_asset/index.html");
        textViewProgress.setVisibility(View.INVISIBLE);
        radioButtonHome.setVisibility(View.INVISIBLE);
        radioButtonStreet.setVisibility(View.INVISIBLE);
        switchPump.setVisibility(View.INVISIBLE);
        textViewPumpTimeTitle.setVisibility(View.INVISIBLE);
        textViewPumpTimeNum.setVisibility(View.INVISIBLE);

        // view на второй вкладке
        progressBarServer2 = findViewById(R.id.progressBarServer2);
        gLAviaryButton = findViewById(R.id.buttonGLAviary);
        switchCHLight = findViewById(R.id.switchCHLight);

        // Делаем невидимым все на второй вкладке, кроме загрузки
        gLAviaryButton.setVisibility(View.INVISIBLE);
        switchCHLight.setVisibility(View.INVISIBLE);

        if (switchPump != null)
        {
            switchPump.setOnCheckedChangeListener(this::onPumpCheckedChanged);
        }

        if (switchCHLight != null)
        {
            switchCHLight.setOnCheckedChangeListener(this::onCHLightCheckedChanged);
        }

        connection1 = new Connection(SERVER1_IP, SERVER1_PORT, "[R]");
        connection2 = new Connection(SERVER2_IP, SERVER2_PORT, "{R}");
    }

    class Connection
    {
        private final String SERVER_IP;
        private final int SERVER_PORT;
        private PrintWriter outputServer;
        private BufferedReader inputServer;
        private final String refreshCommand;
        Thread Thread1 = null;

        public Connection(String SERVER_IP, int SERVER_PORT, String refreshCommand)
        {
            this.SERVER_IP = SERVER_IP;
            this.SERVER_PORT = SERVER_PORT;
            this.refreshCommand = refreshCommand;
            Thread1 = new Thread(new Thread1Server());
            Thread1.start();
        }

        class Thread1Server implements Runnable
        {
            public void run()
            {
                Socket socket;
                try
                {
                    socket = new Socket(SERVER_IP, SERVER_PORT);
                    outputServer = new PrintWriter(socket.getOutputStream());
                    inputServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    new Thread(new Thread2Server()).start();
                    sendDataToServer(refreshCommand);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        class Thread2Server implements Runnable
        {
            @Override
            public void run()
            {
                while (true)
                {
                    try
                    {
                        final String message = inputServer.readLine();
                        if (message != null)
                        {
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    processingInputStream(message);
                                }
                            });
                        }
                        else
                        {
                            Thread1 = new Thread(new Thread1Server());
                            Thread1.start();
                            return;
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        class Thread3Server implements Runnable
        {
            private String message;
            Thread3Server(String message)
            {
                this.message = message;
            }
            @Override
            public void run()
            {
                outputServer.write(message);
                outputServer.flush();
            }
        }

        public void sendDataToServer(String text)
        {
            text = text + "\n";
            new Thread(new Thread3Server(text)).start();
        }

        private void processingInputStream(String text)
        {
            if (text.charAt(0) == 'X')
            {
                String percentString = text.substring(1);
                try
                {
                    int percentInt = Integer.parseInt(percentString);
                    refreshPercentJS(percentInt);
                    isProgressBarWater = true;
                }
                catch (Exception e)
                {
                    Toast toast = Toast.makeText(getApplicationContext(), "Ошибка распознания процентов!", Toast.LENGTH_SHORT);
                    toast.show();
                }
                percentString = percentString + "%";
                textViewProgress.setText(percentString);
            }
            else if (text.charAt(0) == 'T')
            {
                String pumpTimeString = text.substring(1);
                pumpTimeString = pumpTimeString + " минут.";
                textViewPumpTimeNum.setText(pumpTimeString);
                isTextViewPumpTimeNum = true;
            }
            else
            {
                switch (text)
                {
                    case "HOME":
                        if (!radioButtonHome.isChecked())
                        {
                            radioButtonHome.setChecked(true);
                            radioButtonStreet.setChecked(false);
                            switchPump.setClickable(false);
                            isRadioButtonMode = true;
                        }
                        break;

                    case "STREET":
                        if (!radioButtonStreet.isChecked())
                        {
                            radioButtonHome.setChecked(false);
                            radioButtonStreet.setChecked(true);
                            switchPump.setClickable(true);
                            isRadioButtonMode = true;
                        }
                        break;

                    case "PON":
                        switchPump.setChecked(true);
                        textViewPumpTimeTitle.setText("Насос работает:");
                        isSwitchPump = true;
                        break;

                    case "POFF":
                        switchPump.setChecked(false);
                        textViewPumpTimeTitle.setText("Насос работал:");
                        isSwitchPump = true;
                        break;

                    case "CHON":
                        switchCHLight.setChecked(true);
                        isSwitchCHLight = true;
                        break;

                    case "CHOFF":
                        switchCHLight.setChecked(false);
                        isSwitchCHLight = true;
                        break;

                    default:
                        break;
                }
            }
            isFirstScreenReady();
            isSecondScreenReady();
        }

        void isFirstScreenReady()
        {
            if (isProgressBarWater && isRadioButtonMode && isSwitchPump && isTextViewPumpTimeNum)
            {
//                int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
//                switch (currentNightMode)
//                {
//                    case Configuration.UI_MODE_NIGHT_NO:
//                        // ночная тема не активна, используется светлая тема
//                        lightThemeJS();
//                        break;
//                    case Configuration.UI_MODE_NIGHT_YES:
//                        // ночная тема активна, и она используется
//                        darkThemeJS();
//                        break;
//                }
                ColorStateList colorStateList = textViewPumpTimeTitle.getTextColors();
                int color = colorStateList.getDefaultColor();
                progressBarServer1.setVisibility(View.INVISIBLE);
                if (color != -1979711488)       // ночная тема
                {
                    darkThemeJS();
                }
                else        // дневная тема
                {
                    lightThemeJS();
                }
                webViewWater.setVisibility(View.VISIBLE);
                textViewProgress.setVisibility(View.VISIBLE);
                radioButtonHome.setVisibility(View.VISIBLE);
                radioButtonStreet.setVisibility(View.VISIBLE);
                switchPump.setVisibility(View.VISIBLE);
                textViewPumpTimeTitle.setVisibility(View.VISIBLE);
                textViewPumpTimeNum.setVisibility(View.VISIBLE);
            }
        }

        void isSecondScreenReady()
        {
            if (isSwitchCHLight)
            {
                progressBarServer2.setVisibility(View.INVISIBLE);
                gLAviaryButton.setVisibility(View.VISIBLE);
                switchCHLight.setVisibility(View.VISIBLE);
            }
        }

        private void refreshPercentJS(int percent)
        {
            webViewWater.evaluateJavascript("document.getElementById(\"p1\").innerHTML = \".wave:after,.wave:before{top: " + (50 - percent) + "%;}\"",
                    s -> {

                    });
        }

        private void lightThemeJS()
        {
            webViewWater.evaluateJavascript("document.body.style.backgroundColor = \"#fff\";" +
                            "let circleElement = document.querySelector('.circle');" +
                            "circleElement.style.borderColor=\"#fff\"",
                    s -> {

                    });
        }

        private void darkThemeJS()
        {
            webViewWater.evaluateJavascript("document.body.style.backgroundColor = \"#121212\";" +
                            "let circleElement = document.querySelector('.circle');" +
                            "circleElement.style.borderColor=\"#121212\"",
                    s -> {

                    });
        }
    }

    View.OnClickListener radioButtonClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            RadioButton rb = (RadioButton)v;
            switch (rb.getId())
            {
                case R.id.radioButtonModeHome:
                    connection1.sendDataToServer("[HOME]");     // home mode
                    switchPump.setClickable(false);
                    break;

                case R.id.radioButtonModeStreet:
                    connection1.sendDataToServer("[STREET]");       // street mode
                    switchPump.setClickable(true);
                    break;

                default:
                    break;
            }
        }
    };

    public void onPumpCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        if (isChecked)
        {
            connection1.sendDataToServer("[PON]");      // pump on
        }
        if (!isChecked)
        {
            connection1.sendDataToServer("[POFF]");     // pump off
        }
    }

    public void onClickRefresh(View view)
    {
        int tabId = tabHost.getCurrentTab();
        if (tabId == 0)
        {
            connection1 = new Connection(SERVER1_IP, SERVER1_PORT, "[R]");
        }
        if (tabId == 1)
        {
            connection2 = new Connection(SERVER2_IP, SERVER2_PORT, "{R}");
        }
    }

    public void onClickSwitchGeneralLightInAviary(View view)
    {
        connection2.sendDataToServer("{GLAVIARY}");
    }

    public void onCHLightCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        if (isChecked)
        {
            connection2.sendDataToServer("{CHON}");
        }
        if (!isChecked)
        {
            connection2.sendDataToServer("{CHOFF}");
        }
    }
}
