package com.radioyps.doorcontroller;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Created by developer on 28/09/16.
 */
public class PingControllerService extends IntentService {

    private final static String TAG = PingControllerService.class.getSimpleName();
    private final static int RESULT_SUCCESS = 0x11;
    private final static int RESULT_IO_ERROR = 0x12;
    private final static int RESULT_TIMEOUT = 0x13;

    private final static int RESULT_HOST_UNAVAILABLE = 0x14;
    private final static int RESULT_HOST_REFUSED = 0x15;
    private final static int RESULT_NETWORK_UNREACHABLE = 0x16;
    private final static int RESULT_HOSTNAME_NOT_FOUND = 0x17;
    private final static int RESULT_UNKNOWN = 0x18;

    private final static String EXCEPTION_NETWORK_UNREACHABLE = "ENETUNREACH";
    private final static String EXCEPTION_HOST_UNAVAILABLE = "EHOSTUNREACH";
    private final static String EXCEPTION_HOST_REFUSED = "ECONNREFUSED";
    private static int response = RESULT_UNKNOWN;
    private static boolean isContinueConnect = true;

    public PingControllerService() {
        super("com.radioyps.doorcontroller");
    }

    public static void enableConnect(){
//        isContinueConnect = true;
        isContinueConnect = false;
    }

    public static void disableConnect(){
        isContinueConnect = false;
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();

        if (action.equals(CommonConstants.ACTION_PING)) {
            /*
            MainActivity.sendMessage(CommonConstants.MSG_UPDATE_CMD_STATUS, "sending Ping cmd");
            Log.d(TAG, "onHandleIntent()>> sending Ping cmd ");
            if(isControllerAlive()) {
                MainActivity.sendMessage(CommonConstants.MSG_UPDATE_CMD_STATUS, "successfully received ack on ping");
                Log.d(TAG, "onHandleIntent()>> successfully received ack on ping ");
            }else {
                MainActivity.sendMessage(CommonConstants.MSG_UPDATE_CMD_STATUS, "failed to have ack on ping");
                Log.d(TAG, "onHandleIntent()>> failed to have ack on ping ");
            }*/
            connectController();
        } else if (action.equals(CommonConstants.ACTION_PRESS_DOOR_BUTTON)) {
            if(isControllerAlive()) {
                MainActivity.sendMessage(CommonConstants.MSG_UPDATE_BUTTON_STATUS, CommonConstants.DISABLE_BUTTON);
                MainActivity.sendMessage(CommonConstants.MSG_UPDATE_CMD_STATUS, getString(R.string.cmd_in_progress));
                String receviedStr = sendCmd(CommonConstants.CMD_PRESS_DOOR_BUTTON);


                Log.d(TAG, "onHandleIntent()>> sending button press cmd ");
                if (receviedStr.equalsIgnoreCase(CommonConstants.ACK_PRESS_DOOR_BUTTON)) {
                    MainActivity.sendMessage(CommonConstants.MSG_UPDATE_CMD_STATUS, getString(R.string.result_cmd_success));
                    MainActivity.sendMessage(CommonConstants.MSG_UPDATE_BUTTON_STATUS, CommonConstants.ENABLE_BUTTON);
                    Log.d(TAG, "onHandleIntent()>> success on sending button press cmd ");
                } else {
                /*
                MainActivity.sendMessage(CommonConstants.MSG_UPDATE_CMD_STATUS, "failed on sending button press cmd");
                Log.d(TAG, "onHandleIntent()>> failed on sending button press cmd ");
                */
                    connectController();
                }
            }else {
                connectController();
            }


        }else if(action.equals(CommonConstants.ACTION_PRESS_REMOTE_BUTTON)){
           Log.d(TAG, "onHandleIntent()>> GCM sending ");
           MainActivity.sendMessage(CommonConstants.MSG_GCM_CMD_STATUS, getString(R.string.remote_door_cmd_in_progress));
//           sendGCM(CommonConstants.GCM_authrized_mesg);
                     //  sendGCM(BuildConfig.DOORCONFIRMKAY);

        }
    }

    private void sendGCM(String message){
        GcmSendTask gcmTask = new GcmSendTask();
        String [] cmd = new String[] {message, ""};
        gcmTask.execute(cmd);
    }


    private void connectController() {
      
	  while((!isControllerAlive())
              && Utils.isWifiConnected(getBaseContext())
              && isContinueConnect){
          try{
              Thread.sleep(5000L);
          }catch (InterruptedException e){
              Log.d(TAG, "connectController()>> thread sleep error ");
          }

	  }

    }


    private boolean isControllerAlive() {
        boolean ret = false;
        MainActivity.sendMessage(CommonConstants.MSG_UPDATE_CMD_STATUS, getString(R.string.on_connecting_controller));
        Log.d(TAG, "onHandleIntent()>> sending Ping cmd ");
        String receviedStr = sendCmd(CommonConstants.CMD_PING_CONTROLLER);
        if (receviedStr.equalsIgnoreCase(CommonConstants.PING_ACK)) {
            MainActivity.sendMessage(CommonConstants.MSG_UPDATE_CMD_STATUS, getString(R.string.success_on_connecting_controller));
            MainActivity.sendMessage(CommonConstants.MSG_UPDATE_BUTTON_STATUS, CommonConstants.ENABLE_BUTTON);
            Log.d(TAG, "onHandleIntent()>> connected");
            ret = true;
        }else {
            MainActivity.sendMessage(CommonConstants.MSG_UPDATE_CMD_STATUS, receviedStr);
            MainActivity.sendMessage(CommonConstants.MSG_UPDATE_BUTTON_STATUS, CommonConstants.DISABLE_BUTTON);
        }
        return ret;
    }


    private String getStatusString(int status) {
        String ret = getString(R.string.result_cmd_unknown);
        switch (status) {
            case RESULT_HOST_REFUSED:
                ret = getString(R.string.result_cmd_host_refused);
                break;
            case RESULT_HOST_UNAVAILABLE:
                ret = getString(R.string.result_cmd_host_unavailable);
                break;
            case RESULT_HOSTNAME_NOT_FOUND:
                ret = getString(R.string.result_cmd_hostname_not_found);
                break;
            case RESULT_IO_ERROR:
                ret = getString(R.string.result_cmd_io_error);
                break;
            case RESULT_NETWORK_UNREACHABLE:
                ret = getString(R.string.result_cmd_network_unreachable);
                break;
            case RESULT_TIMEOUT:
                ret = getString(R.string.result_cmd_timeout);
                break;
            case RESULT_SUCCESS:
                ret = getString(R.string.result_cmd_success);
                break;
            default:
                ret = getString(R.string.result_cmd_unknown);


        }
        return ret;

    }

    private int getConnectionErrorCode(String error) {
        int ret = RESULT_UNKNOWN;

        if (error.indexOf(EXCEPTION_NETWORK_UNREACHABLE) != -1) {
            ret = RESULT_NETWORK_UNREACHABLE;
        } else if (error.indexOf(EXCEPTION_HOST_UNAVAILABLE) != -1) {
            ret = RESULT_HOST_UNAVAILABLE;
        } else if (error.indexOf(EXCEPTION_HOST_REFUSED) != -1) {
            ret = RESULT_HOST_REFUSED;
        }
        return ret;

    }

    private String getConnectionError(ConnectException ex) {


        String ret = null;

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionStr = sw.toString();
        String lines[] = exceptionStr.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].indexOf("ConnectException") != -1) {
                ret = lines[i];
                break;
            }
        }


        Log.d(TAG, "getConnectionError()>> Line: " + ret);
        return ret;

    }


    private String sendCmd(String cmd) {

        Socket socket = null;
        String stringReceived = "";


        try {

            response = RESULT_UNKNOWN;

            socket = new Socket();

            String ipAddress = Utils.getPreferredIPAdd(getBaseContext());
            int port = Utils.getPreferredIPPort(getBaseContext());

            socket.connect(new InetSocketAddress(ipAddress, port),
                    CommonConstants.SOCKET_CONNECT_TIMEOUT);

            socket.setSoTimeout(CommonConstants.SOCKET_READ_TIMEOUT);

            ByteArrayOutputStream byteArrayOutputStream =
                    new ByteArrayOutputStream(1024);

            byte[] buffer = new byte[1024];

            int bytesRead;
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();


            outputStream.write(cmd.getBytes());
            outputStream.flush();


            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
                stringReceived += byteArrayOutputStream.toString("UTF-8");
            }
            outputStream.close();
            inputStream.close();
            response = RESULT_SUCCESS;

        } catch (ConnectException e) {
            e.printStackTrace();
            String errorStr = getConnectionError(e);
            if (errorStr != null)
                response = getConnectionErrorCode(errorStr);
            else
                response = RESULT_UNKNOWN;

        } catch (UnknownHostException e) {
            e.printStackTrace();
            response = RESULT_HOSTNAME_NOT_FOUND;

        } catch (SocketTimeoutException e) {

            e.printStackTrace();
            response = RESULT_TIMEOUT;

        } catch (IOException e) {
            e.printStackTrace();
            response = RESULT_IO_ERROR;


        } finally {

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        String ret = null;
        if(response == RESULT_SUCCESS){
            ret = stringReceived;
        }else {
             ret = getStatusString(response);

        }
        Log.d(TAG, "sendCmd()>> reply with " + ret);
        return ret;

    }
}

