/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package CekCuacaApp;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class AplikasiCekCuaca extends JFrame {

    private JTextField txtKota;
    private JButton btnCekCuaca, btnSimpanFavorit, btnMuatData;
    private JComboBox<String> cmbFavorit;
    private JLabel lblHasil, lblIcon;
    private JTable tabel;
    private DefaultTableModel model;

    // Daftar file penyimpanan
    private final String apiKey = "c89f4bb0a2cb6a90a3a86789d7abde6d";
    private final String fileFavorit = "favorit.txt";
    private final String fileCSV = "data_cuaca.csv";

    public AplikasiCekCuaca() {
        setTitle("Aplikasi Cek Cuaca");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Panel atas (input dan tombol)
        JPanel panelAtas = new JPanel();
        panelAtas.add(new JLabel("Masukkan Kota:"));
        txtKota = new JTextField(15);
        panelAtas.add(txtKota);

        btnCekCuaca = new JButton("Cek Cuaca");
        panelAtas.add(btnCekCuaca);

        cmbFavorit = new JComboBox<>();
        cmbFavorit.addItem("Pilih kota favorit");
        panelAtas.add(cmbFavorit);

        btnSimpanFavorit = new JButton("Simpan Favorit");
        panelAtas.add(btnSimpanFavorit);

        btnMuatData = new JButton("Muat Data");
        panelAtas.add(btnMuatData);

        add(panelAtas, BorderLayout.NORTH);

        // Panel tengah (hasil cuaca dan gambar)
        JPanel panelTengah = new JPanel(new BorderLayout());
        lblHasil = new JLabel("Data cuaca akan tampil di sini", JLabel.CENTER);
        lblHasil.setFont(new Font("Arial", Font.BOLD, 14));
        panelTengah.add(lblHasil, BorderLayout.NORTH);

        lblIcon = new JLabel("", JLabel.CENTER);
        panelTengah.add(lblIcon, BorderLayout.CENTER);
        add(panelTengah, BorderLayout.CENTER);

        // Panel bawah (tabel data cuaca)
        model = new DefaultTableModel(new String[]{"Kota", "Suhu (°C)", "Kelembapan (%)", "Cuaca"}, 0);
        tabel = new JTable(model);
        add(new JScrollPane(tabel), BorderLayout.SOUTH);

        // Muat kota favorit saat program mulai
        loadFavorit();

        // Tambahkan event
        btnCekCuaca.addActionListener(e -> cekCuaca());
        btnSimpanFavorit.addActionListener(e -> simpanFavorit());
        btnMuatData.addActionListener(e -> muatDataCSV());
        cmbFavorit.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && cmbFavorit.getSelectedIndex() > 0) {
                txtKota.setText(cmbFavorit.getSelectedItem().toString());
            }
        });
    }

    private void cekCuaca() {
        String kota = txtKota.getText().trim();
        if (kota.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan nama kota terlebih dahulu!");
            return;
        }

        try {
            String urlStr = "https://api.openweathermap.org/data/2.5/weather?q=" + kota + "&appid=" + apiKey + "&units=metric&lang=id";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();

            // Parsing manual (tanpa org.json)
            String data = result.toString();

            double suhu = ambilNilaiDouble(data, "\"temp\":", ",");
            int kelembapan = (int) ambilNilaiDouble(data, "\"humidity\":", ",");
            String kondisi = ambilTeks(data, "\"description\":\"", "\"");
            String icon = ambilTeks(data, "\"icon\":\"", "\"");

            lblHasil.setText("Kota: " + kota + " | Suhu: " + suhu + "°C | Kelembapan: " + kelembapan + "% | " + kondisi);
            tampilkanIcon(icon);

            model.addRow(new Object[]{kota, suhu, kelembapan, kondisi});
            simpanDataCSV(kota, suhu, kelembapan, kondisi);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal mengambil data cuaca!\n" + ex.getMessage());
        }
    }

    private double ambilNilaiDouble(String data, String key, String pembatas) {
        try {
            String[] potong = data.split(key);
            if (potong.length > 1) {
                String nilai = potong[1].split(pembatas)[0];
                return Double.parseDouble(nilai);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private String ambilTeks(String data, String key, String pembatas) {
        try {
            String[] potong = data.split(key);
            if (potong.length > 1) {
                return potong[1].split(pembatas)[0];
            }
        } catch (Exception ignored) {}
        return "-";
    }

    private void tampilkanIcon(String icon) {
        try {
            String url = "https://openweathermap.org/img/wn/" + icon + "@2x.png";
            ImageIcon img = new ImageIcon(new URL(url));
            lblIcon.setIcon(img);
        } catch (Exception e) {
            lblIcon.setText("Gagal memuat ikon");
        }
    }

    private void simpanFavorit() {
        String kota = txtKota.getText().trim();
        if (!kota.isEmpty()) {
            try (FileWriter fw = new FileWriter(fileFavorit, true)) {
                fw.write(kota + "\n");
                cmbFavorit.addItem(kota);
                JOptionPane.showMessageDialog(this, "Kota ditambahkan ke favorit!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Gagal menyimpan kota favorit.");
            }
        }
    }

    private void loadFavorit() {
        try (Scanner sc = new Scanner(new File(fileFavorit))) {
            while (sc.hasNextLine()) {
                cmbFavorit.addItem(sc.nextLine());
            }
        } catch (Exception e) {
            // file belum ada, abaikan
        }
    }

    private void simpanDataCSV(String kota, double suhu, int kelembapan, String kondisi) {
        try (FileWriter fw = new FileWriter(fileCSV, true)) {
            fw.write(kota + "," + suhu + "," + kelembapan + "," + kondisi + "\n");
        } catch (IOException e) {
            System.out.println("Gagal menyimpan ke CSV");
        }
    }

    private void muatDataCSV() {
        model.setRowCount(0);
        try (BufferedReader br = new BufferedReader(new FileReader(fileCSV))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                model.addRow(data);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Belum ada data tersimpan!");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AplikasiCekCuaca().setVisible(true));
    }
}
