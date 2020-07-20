package firebase.app.laboratorio_11;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
        private static final String TAG = "VoiceRecord";
        String pathSave = "";
        private static final int RECORDER_SAMPLERATE = 8000;
        private static final int RECORDER_CHANNELS_IN = AudioFormat.CHANNEL_IN_MONO;
        private static final int RECORDER_CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
        private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
        private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;

        private int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS_IN, RECORDER_AUDIO_ENCODING);
        final int REQUEST_PERMISSION_CODE = 1000;

        private AudioRecord recorder = null;
        private Thread recordingThread = null;
        private boolean isRecording = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            if(!checkPermissioFromDevide())
                requestPermissions();

            findViewById(R.id.btnStartRecord).setOnClickListener(btnClick);
            findViewById(R.id.btnStopRecord).setOnClickListener(btnClick);
            enableButtons(false);
        }

        private void enableButton(int id, boolean isEnable) {

            findViewById(id).setEnabled(isEnable);
        }

        private void enableButtons(boolean isRecording) {
            enableButton(R.id.btnStartRecord, !isRecording);
            enableButton(R.id.btnStopRecord, isRecording);

        }

        private void startRecording() {
            if(bufferSize == AudioRecord.ERROR_BAD_VALUE)
                Log.e(TAG, "Bad Value for \"bufferSize\", recording parameters are not supported by the hardware");

            if(bufferSize == AudioRecord.ERROR)
                Log.e(TAG, "Bad Value for \"bufferSize\", implementation was unable to query the hardware for its output properties");

            Log.e(TAG, "\"bufferSize\"="+bufferSize);


            recorder = new AudioRecord(AUDIO_SOURCE, RECORDER_SAMPLERATE, RECORDER_CHANNELS_IN, RECORDER_AUDIO_ENCODING, bufferSize);
            recorder.startRecording();
            Toast.makeText(MainActivity.this, "Recording...", Toast.LENGTH_SHORT).show();

            isRecording = true;

            recordingThread = new Thread(new Runnable() {
                public void run() {
                    writeAudioDataToFile();
                }
            }, "AudioRecorder Thread");
            recordingThread.start();
        }

        private void writeAudioDataToFile() {
            String filePath = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/"+ UUID.randomUUID().toString()+"_audio_record.pcm";
            pathSave = filePath;
            byte saudioBuffer[] = new byte[bufferSize];

            FileOutputStream os = null;
            try {
                os = new FileOutputStream(filePath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            while (isRecording) {
                recorder.read(saudioBuffer, 0, bufferSize);
                try {
                    os.write(saudioBuffer, 0, bufferSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void stopRecording() throws IOException {
            if (null != recorder) {
                isRecording = false;
                recorder.stop();
                Toast.makeText(MainActivity.this, "Stop...", Toast.LENGTH_SHORT).show();
                recorder.release();
                recorder = null;
                recordingThread = null;
                PlayShortAudioFileViaAudioTrack(pathSave);
            }
        }

        private void PlayShortAudioFileViaAudioTrack(String filePath) throws IOException{
            if (filePath==null)
                return;

            File file = new File(filePath);
            byte[] byteData = new byte[(int) file.length()];
            Log.d(TAG, (int) file.length()+"");

            FileInputStream in = null;
            try {
                in = new FileInputStream(file);
                in.read(byteData);
                in.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            int intSize = android.media.AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS_OUT, RECORDER_AUDIO_ENCODING);
            Log.d(TAG, intSize+"");

            AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS_OUT, RECORDER_AUDIO_ENCODING, intSize, AudioTrack.MODE_STREAM);
            if (at!=null) {
                at.play();
                at.write(byteData, 0, byteData.length);
                at.stop();
                at.release();
            }
            else
                Log.d(TAG, "audio track is not initialised ");

        }

        private View.OnClickListener btnClick = new View.OnClickListener() {
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btnStartRecord: {
                        enableButtons(true);
                        startRecording();
                        break;
                    }
                    case R.id.btnStopRecord: {
                        enableButtons(false);
                        try {
                            stopRecording();

                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        };

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                finish();
            }
            return super.onKeyDown(keyCode, event);
        }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        }, REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_PERMISSION_CODE:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this,"Permission Granted", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this,"Permission Denied", Toast.LENGTH_SHORT).show();
            }
                break;
        }
    }

    private boolean checkPermissioFromDevide() {
        int write_external_storage_result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int record_audio_result = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return write_external_storage_result == PackageManager.PERMISSION_GRANTED &&
                record_audio_result == PackageManager.PERMISSION_GRANTED;
    }
}