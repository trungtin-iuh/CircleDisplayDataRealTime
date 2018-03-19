package com.example.user.circledisplaydatarealtime;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.example.user.circledisplaydatarealtime.model.AddressParameterRealTimeData;
import com.example.user.circledisplaydatarealtime.model.CircleDisplay;
import com.example.user.circledisplaydatarealtime.model.ParameterObjectRealTimeData;
import com.example.user.circledisplaydatarealtime.model.RealTimeStreamingData;
import com.example.user.circledisplaydatarealtime.utils.GsonUtil;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketOptions;

public class MainActivity extends AppCompatActivity implements CircleDisplay.SelectionListener {

    private Button btnConnect;
    private Button btnDisConnect;
    private LinearLayout linearLayout;

    private int startId = 0;

    private Map<String, Integer> mIdMapping = new HashMap<String, Integer>();
    private WebSocketConnection mWebSocketConnection = new WebSocketConnection();
    private static final String TAG =
            MainActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        init_view();
        
        init_onclick();
    }

    private void init_onclick() {
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToServer("");
            }
        });

        btnDisConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disConnectToServer();
            }
        });
    }

    private void disConnectToServer() {
        if (mWebSocketConnection.isConnected()) {
            mWebSocketConnection.disconnect();
        }
        mWebSocketConnection = null;
    }

    private void connectToServer(String wsURL) {
        String webSocketRealTimeDataUri = "wss://dataengine.globiots.com:443/data-engine/mobile/realtime";
        final String messageTogetDataRealTime = "{\"objects\": [{\"addresses\": [{\"address\":\"3000\",\"dataType\":\"integer_16\",\"length\":2,\"value\":\"\"},{\"address\":\"3002\",\"dataType\":\"integer_16\",\"length\":2,\"value\":\"\"},{\"address\":\"3004\",\"dataType\":\"integer_16\",\"length\":2,\"value\":\"\"},{\"address\": \"201A\",\"dataType\": \"float\",\"length\": 4},{\"address\": \"2020\",\"dataType\": \"float\",\"length\": 4},{\"address\": \"2000\",\"dataType\": \"float\",\"length\": 4}],\"hostname\": \"0.0.0.254\"}],\"sessionId\": \"\",\"timezone\": \"GMT+07:00\",\"updateTime\": 3}";

//        // start init data - build layout
        RealTimeStreamingData realTimeStreamingData = GsonUtil.getInstance()
                .fromJson (messageTogetDataRealTime, RealTimeStreamingData.class);

        linearLayout.removeAllViews();

        for(ParameterObjectRealTimeData parameterObjectRealTimeData : realTimeStreamingData.getObjects()) {
            String hostName = parameterObjectRealTimeData.getHostname();

            for (AddressParameterRealTimeData addressParameterRealTimeData : parameterObjectRealTimeData.getAddresses()) {
                mIdMapping.put(hostName + ":" + addressParameterRealTimeData.getAddress(), startId);

                CircleDisplay circleDisplay = new CircleDisplay(getApplicationContext());
                circleDisplay.setId(startId);
                int width=350;
                int height=350;
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width,height);
                params.setMargins(0,8,0,8);
                circleDisplay.setLayoutParams(params);
                linearLayout.addView(circleDisplay);
                startId++;
            }
        }

        // end init data - transport data to layout
        try{
            WebSocket.WebSocketConnectionObserver realTimeRetrieveHandle =
                    new WebSocket.WebSocketConnectionObserver() {

                        @Override
                        public void onOpen() {
                            mWebSocketConnection.sendTextMessage(messageTogetDataRealTime);
                        }

                        @Override
                        public void onTextMessage(final String dataRealTime) {

                            RealTimeStreamingData realTimeStreamingData = GsonUtil.getInstance()
                                    .fromJson (dataRealTime, RealTimeStreamingData.class);

                            for(ParameterObjectRealTimeData parameterObjectRealTimeData : realTimeStreamingData.getObjects()){
                                String hostName =  parameterObjectRealTimeData.getHostname();
                                for(AddressParameterRealTimeData addressParameterRealTimeData: parameterObjectRealTimeData.getAddresses()){

                                    CircleDisplay circleDisplay = linearLayout.findViewById(mIdMapping.get(hostName+":"+addressParameterRealTimeData.getAddress()));
                                    String val = addressParameterRealTimeData.getValue();

                                    //Display in Circle
                                    disPlayInCircle(circleDisplay,val);

                                }
                            }

                        }

                        @Override
                        public void onRawTextMessage(byte[] payload) {

                        }

                        @Override
                        public void onBinaryMessage(byte[] payload) {

                        }

                        @Override
                        public void onClose(final WebSocketCloseNotification code, final String reason) {

                        }
                    };
            WebSocketOptions websocketOptions = new WebSocketOptions();
            websocketOptions.setSocketConnectTimeout(15000);//ms ~ 15 s
            websocketOptions.setSocketReceiveTimeout(15000);//ms ~ 15 s

            mWebSocketConnection.connect(
                    new URI(webSocketRealTimeDataUri),
                    realTimeRetrieveHandle,
                    websocketOptions);

        } catch (final Exception e){
            Log.w("WEB_SOCKET", e.toString());

        }
    }

    private void disPlayInCircle(CircleDisplay circleDisplay, String val) {

        circleDisplay.setValueWidthPercent(55f);
        circleDisplay.setTextSize(14f);
        circleDisplay.setColor(Color.GREEN);
        circleDisplay.setDrawText(true);
        circleDisplay.setDrawInnerCircle(true);
        circleDisplay.setFormatDigits(1);
        circleDisplay.setTouchEnabled(true);
        circleDisplay.setSelectionListener(this);
        circleDisplay.setUnit("%");
        circleDisplay.setStepSize(0.5f);
        circleDisplay.showValue(Float.parseFloat(val), 330f, true);

    }

    private void init_view() {
        btnConnect = findViewById(R.id.btnConnect);
        btnDisConnect = findViewById(R.id.btnDisConnect);
        linearLayout = findViewById(R.id.lineCircle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebSocketConnection.isConnected()) {
            mWebSocketConnection.disconnect();
        }
        mWebSocketConnection = null;
    }

    @Override
    public void onSelectionUpdate(float val, float maxval) {
        Log.i("Main", "Selection update: " + val + ", max: " + maxval);

    }

    @Override
    public void onValueSelected(float val, float maxval) {
        Log.i("Main", "Selection complete: " + val + ", max: " + maxval);


    }
}
