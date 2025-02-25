package com.example.real_time_graph_bluetooth_result_3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button btnConnect;
    private TextView sensorDataText;
    private BarChart chart1, chart2;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice bluetoothDevice;

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int MAX_DATA_POINTS = 16;  // 최대 16개 데이터 포인트
    private int numSensors = 0;
    private int currentIndex = 0;  // 현재 데이터가 들어올 위치

    private float[] sensorDataArray = new float[MAX_DATA_POINTS];  // 센서 데이터 배열 (최대 16개)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        btnConnect = findViewById(R.id.btn_connect);
        sensorDataText = findViewById(R.id.sensor_data);

        showSensorCountDialog();

        chart1 = findViewById(R.id.chart);
        chart2 = findViewById(R.id.chart2);

        btnConnect.setOnClickListener(v -> checkBluetoothConnectPermission());
    }

    private void showSensorCountDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("센서 개수를 입력 (최대 16)");

        new AlertDialog.Builder(this)
                .setTitle("센서 개수 입력")
                .setMessage("몇 개의 센서를 사용하시겠습니까?")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    try {
                        int sensors = Integer.parseInt(input.getText().toString());
                        if (sensors < 1 || sensors > 16) {
                            Toast.makeText(this, "1부터 16까지의 숫자를 입력해주세요.", Toast.LENGTH_SHORT).show();
                        } else {
                            numSensors = sensors;
                            Toast.makeText(this, numSensors + "개의 센서를 사용합니다.", Toast.LENGTH_SHORT).show();
                            setupCharts();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "유효한 숫자를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupCharts() {
        setupChart(chart1, 0, 7);  // chart1: 1~8
        setupChart(chart2, 8, 15);  // chart2: 9~16
    }

    private void setupChart(BarChart chart, int startSensor, int endSensor) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        int[] colorArray = {
                getResources().getColor(R.color.color_blue),
                getResources().getColor(R.color.color_green),
                getResources().getColor(R.color.color_red),
                getResources().getColor(R.color.color_orange),
                getResources().getColor(R.color.color_purple),
                getResources().getColor(R.color.color_teal),
                getResources().getColor(R.color.color_yellow),
                getResources().getColor(R.color.color_brown)
        };

        for (int i = startSensor; i <= endSensor; i++) {
            entries.add(new BarEntry(i - startSensor, 0f));  // 초기값 0f로 설정
            colors.add(colorArray[i - startSensor]);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Sensors " + (startSensor + 1) + " to " + (endSensor + 1));
        dataSet.setColors(colors);

        BarData data = new BarData(dataSet);


        // Set value formatter for 3 decimal places
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.3f", value); // Format to 3 decimal places
            }
        });


        chart.setData(data);
        chart.invalidate();

        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getAxisLeft().setEnabled(true);
        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getXAxis().setTextColor(getResources().getColor(android.R.color.holo_purple));
        // Set value text color to Magenta
        dataSet.setValueTextColor(getResources().getColor(android.R.color.holo_purple));  // Magenta color
        YAxis yAxis = chart.getAxisLeft();
        yAxis.setAxisMinimum(0.0f);
        yAxis.setAxisMaximum(3.3f);
        yAxis.setGranularity(0.1f);
        yAxis.setTextColor(getResources().getColor(android.R.color.holo_purple));
        chart.getLegend().setTextColor(getResources().getColor(android.R.color.holo_purple));
        chart.getDescription().setTextColor(getResources().getColor(android.R.color.holo_purple));
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE
            }, PERMISSION_REQUEST_CODE);
        } else {
            enableBluetooth();
        }
    }

    private void enableBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothAdapter.enable();
        }
    }

    private void checkBluetoothConnectPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST_CODE);
            } else {
                showBluetoothDevices();
            }
        } else {
            showBluetoothDevices();
        }
    }

    private void showBluetoothDevices() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothLeScanner.startScan(scanCallback);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            @SuppressLint("MissingPermission") String deviceName = device.getName();

            // 디바이스 이름이 "CHIPSEN"인 경우에만 연결 시도
            if (deviceName != null && deviceName.equals("CHIPSEN")) {
                Log.d("Bluetooth", "Found CHIPSEN device: " + deviceName);
                connectToBluetoothDevice(device);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(MainActivity.this, "Scan failed: " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    private void connectToBluetoothDevice(BluetoothDevice device) {
        bluetoothDevice = device;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt = bluetoothDevice.connectGatt(this, false, gattCallback);
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("Bluetooth", "GATT connected");
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("Bluetooth", "GATT disconnected");
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("Bluetooth", "Services discovered");

                // 특성에 대해 알림 활성화
                BluetoothGattCharacteristic characteristic = getCharacteristicForReceivingData();
                if (characteristic != null) {
                    // 알림 설정
                    bluetoothGatt.setCharacteristicNotification(characteristic, true);

                    // 특성에 대한 디스크립터 설정 (알림을 활성화)
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        bluetoothGatt.writeDescriptor(descriptor);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            processReceivedData(characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                processReceivedData(characteristic);
            }
        }
    };

    private BluetoothGattCharacteristic getCharacteristicForReceivingData() {
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"));
        if (service != null) {
            return service.getCharacteristic(UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"));
        }
        return null;
    }
    private List<Byte> dataBuffer = new ArrayList<>(); // StringBuilder 대신 List<Byte> 사용
    private boolean isFirstData = true; // 첫 데이터 무시 플래그
    // 데이터 수신 및 처리
    private void processReceivedData(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue(); // 바이트 배열로 데이터 수신
        Log.d("Bluetooth", "Received Raw Data: " + bytesToHex(value)); // 디버깅용: 바이트 배열을 16진수로 출력

        // 바이트 배열을 버퍼에 추가
        dataBuffer.addAll(byteArrayToList(value));

        // "END" 플래그 검색 (바이트 배열에서 "END"는 {0x45, 0x4E, 0x44})
        while (dataBuffer.size() >= 3) { // "END"는 3바이트이므로 최소 3바이트 필요
            int endIndex = findEndIndex(dataBuffer);
            if (endIndex == -1) {
                break; // "END"를 찾지 못한 경우 종료
            }

            // "END" 이전 데이터 블록 추출
            List<Byte> dataBlock = dataBuffer.subList(0, endIndex);
            dataBuffer = dataBuffer.subList(endIndex + 3, dataBuffer.size()); // "END" 이후 데이터로 버퍼 갱신

            Log.d("Bluetooth", "Extracted Data Block: " + bytesToHex(listToByteArray(dataBlock)));

            // 첫 번째 데이터는 무시
            if (isFirstData) {
                Log.d("Bluetooth", "First data ignored.");
                isFirstData = false;
                continue;
            }

            // 2바이트씩 센서 데이터 처리
            parseSensorData(listToByteArray(dataBlock));
        }
    }
    // 센서 데이터 처리
    private void parseSensorData(byte[] dataBytes) {
        Log.d("Bluetooth", "dataBytes length: " + dataBytes.length); // 디버깅: 데이터 길이 확인

        // 센서 데이터가 2바이트씩 들어오므로 센서 수를 계산
        int sensorCount = dataBytes.length / 2;

        // 센서 수가 예상보다 많으면 잘라내기
        if (sensorCount > MAX_DATA_POINTS) {
            Log.w("Bluetooth", "Received more sensors than expected. Trimming to max size.");
            sensorCount = MAX_DATA_POINTS;
        }

        Log.d("Bluetooth", "sensor Count : " + sensorCount);

        float[] parsedData = new float[sensorCount];
        for (int i = 0; i < sensorCount; i++) {
            int A = dataBytes[i * 2] & 0xFF; // 첫 번째 바이트
            int B = dataBytes[i * 2 + 1] & 0xFF; // 두 번째 바이트

            // 센서 값 계산 (A와 B를 결합하여 16비트 값을 만듦 Big-endian 방식)
            //int combinedValue = ((A & 0xFF) << 8) | (B & 0xFF);
            // 센서 값 계산 (B와 A를 결합하여 16비트 값을 만듦, Little-endian 방식)
            int combinedValue = ((B & 0xFF) << 8) | (A & 0xFF);
            combinedValue &= 0xFFF; // 하위 12비트만 사용
            parsedData[i] = combinedValue * 3.3f / 4096f;

            Log.d("Bluetooth", String.format("Sensor %d - A: %d, B: %d, Value: %.3f", i, A, B, parsedData[i]));
        }

        // 센서 데이터 저장
        System.arraycopy(parsedData, 0, sensorDataArray, 0, sensorCount);
        numSensors = sensorCount;

        // UI 업데이트
        runOnUiThread(this::updateCharts);
    }
    // 바이트 배열을 List<Byte>로 변환
    private List<Byte> byteArrayToList(byte[] bytes) {
        List<Byte> list = new ArrayList<>();
        for (byte b : bytes) {
            list.add(b);
        }
        return list;
    }

    // List<Byte>를 바이트 배열로 변환
    private byte[] listToByteArray(List<Byte> list) {
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            bytes[i] = list.get(i);
        }
        return bytes;
    }

    // 바이트 배열에서 "END" 플래그의 위치를 찾음
    private int findEndIndex(List<Byte> dataBuffer) {
        for (int i = 0; i < dataBuffer.size() - 2; i++) {
            if (dataBuffer.get(i) == 0x45 && // 'E'
                    dataBuffer.get(i + 1) == 0x4E && // 'N'
                    dataBuffer.get(i + 2) == 0x44) { // 'D'
                return i;
            }
        }
        return -1; // "END"를 찾지 못한 경우
    }

    // 바이트 배열을 16진수 문자열로 변환 (디버깅용)
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }


    private void updateCharts() {
        // 첫 번째 차트 (1~8)
        for (int i = 0; i < Math.min(numSensors, 8); i++) {  // numSensors가 8 미만일 경우 처리
            BarEntry entry = new BarEntry(i, sensorDataArray[i]);
            updateChartData(chart1, entry, i);
        }

        // 두 번째 차트 (9~16)
        for (int i = 8; i < numSensors; i++) {  // 9~16번 데이터를 두 번째 차트에 표시
            BarEntry entry = new BarEntry(i - 8, sensorDataArray[i]);
            updateChartData(chart2, entry, i - 8);  // 두 번째 차트는 첫 번째 차트의 인덱스를 맞추기 위해 -8을 함
        }
    }


    // 차트 데이터 업데이트
    private void updateChartData(BarChart chart, BarEntry entry, int sensorIndex) {
        BarData data = chart.getData();
        if (data != null) {
            BarDataSet dataSet = (BarDataSet) data.getDataSetByIndex(0);
            if (dataSet != null) {
                dataSet.getEntryForIndex(sensorIndex).setY(entry.getY());
                chart.notifyDataSetChanged();
                chart.invalidate();
            }
        }
    }
}