/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package kasir;

/**
 *
 * @author Acer Aspire Lite 15
 */

import javax.swing.*;
import java.awt.event.*;
import login_system.login;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import login_system.koneksi;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.text.SimpleDateFormat;
import kasir.dashboard_kasir;

public class kasir2 extends javax.swing.JFrame {

    /**
     * Creates new form kasir
     */
    private DecimalFormat decimalFormatter;

    public kasir2() {
        initComponents();

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        this.decimalFormatter = new DecimalFormat("#,###", symbols);

        setupTable();

        setupShortcuts(); // Mengatur shortcut F12

        ksr_tanggal.setDate(new java.util.Date());

        generateTransactionId();

        ksr_cb_statusmember.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMemberFieldsState();
            }
        });

        // Panggil sekali setelah listener ditambahkan
        updateMemberFieldsState();

        // Aksi untuk tombol kembali
        Pa_btn_kembali.addActionListener(e -> {
            new dashboard_kasir().setVisible(true);
            this.dispose(); // Tutup halaman kasir saat ini
        });

        ksr_tf_diskon.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateTotals();
            }
        });

        ksr_tf_bayar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateTotals();
            }
        });

        loadMetodePembayaran();

        // 2. Tambahkan ActionListener ke ComboBox pertama
        ksr_cb_metodepembayaran.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Cek apakah ada item yang dipilih
                if (ksr_cb_metodepembayaran.getSelectedItem() != null) {
                    // Ambil metode yang dipilih
                    String selectedMetode = ksr_cb_metodepembayaran.getSelectedItem().toString();
                    // Panggil method untuk memuat ComboBox kedua berdasarkan pilihan
                    loadAkunPembayaran(selectedMetode);
                }
            }
        });

        // 3. Muat data untuk ComboBox kedua secara otomatis saat form pertama kali
        // dibuka
        // (berdasarkan item pertama yang terpilih di ComboBox pertama)
        if (ksr_cb_metodepembayaran.getItemCount() > 0) {
            loadAkunPembayaran(ksr_cb_metodepembayaran.getItemAt(0));
        }
    }

    private void generateTransactionId() {
        try (Connection conn = koneksi.getConnection();
                // Mengambil id_transaksi terbesar, diurutkan dari besar ke kecil, dan hanya
                // ambil 1 baris
                PreparedStatement pst = conn
                        .prepareStatement("SELECT id_transaksi FROM transaksi ORDER BY id_transaksi DESC LIMIT 1");
                ResultSet rs = pst.executeQuery()) {

            int lastId = 0;
            if (rs.next()) {
                // Jika ada data, ambil nomor terakhir
                lastId = rs.getInt("id_transaksi");
            }

            // Tambahkan 1 untuk mendapatkan nomor baru
            int newId = lastId + 1;

            // Format nomor baru menjadi 5 digit dengan angka nol di depan (misal: 00001)
            String formattedId = String.format("%05d", newId);

            // Set nomor baru ke text field
            ksr_tf_id_transaksi.setText(formattedId);

            // Buat text field tidak bisa diedit
            ksr_tf_id_transaksi.setEditable(false);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal membuat No. Transaksi otomatis: " + e.getMessage());
            // Jika gagal, set default dan buat non-editable
            ksr_tf_id_transaksi.setText("00001");
            ksr_tf_id_transaksi.setEditable(false);
        }
    }

    private void updateMemberFieldsState() {
        // Ambil status yang dipilih dari ComboBox
        String status = ksr_cb_statusmember.getSelectedItem().toString();

        // --- PERUBAHAN LOGIKA DIMULAI DI SINI ---

        // Nonaktifkan kemampuan edit untuk field diskon member secara permanen
        ksr_tf_diskon_member.setEditable(false);

        if (status.equals("AKTIF")) {
            // Jika status AKTIF:
            // 1. Field ID Member bisa diisi
            ksr_tf_id_member.setEnabled(true);

            // 2. Diskon member otomatis di-set ke "5" (untuk 5%)
            ksr_tf_diskon_member.setText("5");

        } else { // Jika "TIDAK AKTIF"
            // Jika status TIDAK AKTIF:
            // 1. Field ID Member tidak bisa diisi dan dikosongkan
            ksr_tf_id_member.setEnabled(false);
            ksr_tf_id_member.setText("");

            // 2. Diskon member di-set ke "0" (atau kosong)
            ksr_tf_diskon_member.setText("0");
        }

        // Panggil updateTotals() agar seluruh kalkulasi diperbarui secara otomatis
        updateTotals();
    }

    private void loadMetodePembayaran() {
        try (Connection conn = login_system.koneksi.getConnection();
                // --- PERBAIKAN DI SINI ---
                PreparedStatement pst = conn
                        .prepareStatement("SELECT nama_metode FROM \"metodepembayaran\" ORDER BY nama_metode");
                ResultSet rs = pst.executeQuery()) {

            // Kosongkan item lama
            ksr_cb_metodepembayaran.removeAllItems();

            // Tambahkan item baru dari database
            while (rs.next()) {
                ksr_cb_metodepembayaran.addItem(rs.getString("nama_metode"));
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat metode pembayaran: " + e.getMessage());
        }
    }

    private void loadAkunPembayaran(String metode) {
        String sql = "SELECT nama_pembayaran FROM \"akunPembayaran\" WHERE metode_pembayaran = ? ORDER BY nama_pembayaran";

        try (Connection conn = login_system.koneksi.getConnection();
                PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, metode); // Set parameter WHERE clause

            try (ResultSet rs = pst.executeQuery()) {
                // Kosongkan item lama
                ksr_cb_pembayaran.removeAllItems();

                // Tambahkan item baru yang sudah difilter
                while (rs.next()) {
                    ksr_cb_pembayaran.addItem(rs.getString("nama_pembayaran"));
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat akun pembayaran: " + e.getMessage());
        }
    }

    private void setupTable() {
        DefaultTableModel model = new DefaultTableModel() {
            // Membuat sel tabel tidak bisa diedit langsung
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tbl_daftarmenu.setModel(model);

        model.addColumn("ID Menu"); // Kolom untuk data, tapi akan disembunyikan
        model.addColumn("Nama Menu");
        model.addColumn("Harga");
        model.addColumn("Jumlah");
        model.addColumn("Total");
        model.addColumn("Keterangan");

        // --- TAMBAHAN --- Sembunyikan kolom ID Menu dari tampilan
        TableColumn idColumn = tbl_daftarmenu.getColumnModel().getColumn(0);
        idColumn.setMinWidth(0);
        idColumn.setMaxWidth(0);
        idColumn.setWidth(0);
    }

    // Lokasi: Ganti seluruh method updateTotals() yang lama dengan ini

    private void updateTotals() {
        DefaultTableModel model = (DefaultTableModel) tbl_daftarmenu.getModel();
        long subtotal = 0;

        // 1. Hitung Subtotal (ini masih sama)
        for (int i = 0; i < model.getRowCount(); i++) {
            String totalString = model.getValueAt(i, 4).toString().replace(".", "");
            subtotal += Long.parseLong(totalString);
        }
        ksr_tf_subtotal.setText(decimalFormatter.format(subtotal));

        // --- PERUBAHAN LOGIKA DISKON DIMULAI DI SINI ---

        // 2. Hitung Diskon Reguler (%)
        double diskonRegulerPersen = 0;
        String teksDiskonReguler = ksr_tf_diskon.getText();
        if (!teksDiskonReguler.isEmpty()) {
            try {
                diskonRegulerPersen = Double.parseDouble(teksDiskonReguler);
            } catch (NumberFormatException e) {
                // Biarkan 0 jika input tidak valid
            }
        }
        // Hitung jumlah diskon dalam nominal
        long diskonRegulerAmount = (long) (subtotal * (diskonRegulerPersen / 100.0));

        // 3. Hitung Diskon Member (%)
        double diskonMemberPersen = 0;
        // Cek dulu apakah field diskon member aktif
        if (ksr_tf_diskon_member.isEnabled()) {
            String teksDiskonMember = ksr_tf_diskon_member.getText();
            if (!teksDiskonMember.isEmpty()) {
                try {
                    diskonMemberPersen = Double.parseDouble(teksDiskonMember);
                } catch (NumberFormatException e) {
                    // Biarkan 0 jika input tidak valid
                }
            }
        }
        // Hitung jumlah diskon dalam nominal
        long diskonMemberAmount = (long) (subtotal * (diskonMemberPersen / 100.0));

        // 4. Hitung Grand Total
        long totalDiskon = diskonRegulerAmount + diskonMemberAmount;
        long grandTotal = subtotal - totalDiskon;

        ksr_tf_total2.setText(decimalFormatter.format(grandTotal));
        ksr_tf_total1.setText(decimalFormatter.format(grandTotal));

        // 5. Hitung Kembalian (ini masih sama)
        long bayar = 0;
        String teksBayar = ksr_tf_bayar.getText();
        if (!teksBayar.isEmpty()) {
            try {
                bayar = Long.parseLong(teksBayar.replace(".", ""));
            } catch (NumberFormatException e) {
                // Biarkan 0 jika input bayar tidak valid
            }
        }
        long kembalian = bayar - grandTotal;
        ksr_tf_kembalian.setText(decimalFormatter.format(kembalian));
    }

    private void setupShortcuts() {
        JRootPane rootPane = this.getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        // Shortcut untuk Pilih Menu (F12)
        String openMenuActionKey = "OPEN_MENU_ACTION";
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), openMenuActionKey);
        actionMap.put(openMenuActionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btn_pilih_menuActionPerformed(e);
            }
        });

        // Shortcut untuk tombol Edit (F2)
        String editActionKey = "EDIT_ACTION";
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), editActionKey);
        actionMap.put(editActionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btn_editActionPerformed(e);
            }
        });
    }

    public void addMenuItemToTable(Object[] rowData) {
        DefaultTableModel model = (DefaultTableModel) tbl_daftarmenu.getModel();
        model.addRow(rowData);
        updateTotals();
    }

    public void updateMenuItemInTable(int rowIndex, Object[] rowData) {
        DefaultTableModel model = (DefaultTableModel) tbl_daftarmenu.getModel();
        model.setValueAt(rowData[0], rowIndex, 0); // Kolom 0: ID Menu
        model.setValueAt(rowData[1], rowIndex, 1); // Kolom 1: Nama Menu
        model.setValueAt(rowData[2], rowIndex, 2); // Kolom 2: Harga
        model.setValueAt(rowData[3], rowIndex, 3); // Kolom 3: Jumlah
        model.setValueAt(rowData[4], rowIndex, 4); // Kolom 4: Total
        model.setValueAt(rowData[5], rowIndex, 5); // Kolom 5: Keterangan
        updateTotals();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        ksr_tf_id_member = new javax.swing.JTextField();
        JPanelmenu = new javax.swing.JPanel();
        btn_pilih_menu = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tbl_daftarmenu = new javax.swing.JTable();
        btn_pilih_inventory = new javax.swing.JButton();
        btn_edit = new javax.swing.JButton();
        btn_delete = new javax.swing.JButton();
        btn_cancel = new javax.swing.JButton();
        btn_save = new javax.swing.JButton();
        panel_action_bar = new javax.swing.JPanel();
        Pa_btn_kembali = new javax.swing.JButton();
        Pa_txt1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        ksr_tanggal = new com.toedter.calendar.JDateChooser();
        jLabel22 = new javax.swing.JLabel();
        ksr_tf_total1 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        ksr_tf_diskon = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        ksr_tf_diskon_member = new javax.swing.JTextField();
        jLabel23 = new javax.swing.JLabel();
        ksr_tf_total2 = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        ksr_tf_id_transaksi = new javax.swing.JTextField();
        ksr_tf_pelayan = new javax.swing.JTextField();
        ksr_tf_pembeli = new javax.swing.JTextField();
        ksr_tf_subtotal = new javax.swing.JTextField();
        ksr_tf_kembalian = new javax.swing.JTextField();
        ksr_tf_bayar = new javax.swing.JTextField();
        ksr_cb_pembayaran = new javax.swing.JComboBox<>();
        ksr_cb_statusmember = new javax.swing.JComboBox<>();
        ksr_cb_metodepembayaran = new javax.swing.JComboBox<>();
        jScrollPane2 = new javax.swing.JScrollPane();
        ksr_ta_keterangan = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1280, 780));
        setSize(new java.awt.Dimension(1100, 600));
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel1.setText("NO. TRANSAKSI :");
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 70, 130, 30));

        ksr_tf_id_member.setDragEnabled(true);
        ksr_tf_id_member.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ksr_tf_id_memberActionPerformed(evt);
            }
        });
        getContentPane().add(ksr_tf_id_member, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 150, 210, 30));

        JPanelmenu.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2));
        JPanelmenu.setPreferredSize(new java.awt.Dimension(1047, 170));
        JPanelmenu.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        btn_pilih_menu.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btn_pilih_menu.setText("[F12] Pilih Menu");
        btn_pilih_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_pilih_menuActionPerformed(evt);
            }
        });
        JPanelmenu.add(btn_pilih_menu, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, 160, 30));

        tbl_daftarmenu.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "nama menu", "harga", "jumlah", "total", "keterangan"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tbl_daftarmenu.setRowHeight(30);
        jScrollPane1.setViewportView(tbl_daftarmenu);

        JPanelmenu.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 60, 1220, 160));

        btn_pilih_inventory.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btn_pilih_inventory.setText("[F11] Pilih Inventory");
        btn_pilih_inventory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_pilih_inventoryActionPerformed(evt);
            }
        });
        JPanelmenu.add(btn_pilih_inventory, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 10, 180, 30));

        btn_edit.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btn_edit.setText("[F2] Edit");
        btn_edit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_editActionPerformed(evt);
            }
        });
        JPanelmenu.add(btn_edit, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 10, 160, 30));

        btn_delete.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btn_delete.setText("[Del] Delete");
        btn_delete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_deleteActionPerformed(evt);
            }
        });
        JPanelmenu.add(btn_delete, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 10, 160, 30));

        getContentPane().add(JPanelmenu, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 200, 1280, 250));

        btn_cancel.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btn_cancel.setText("CANCEL");
        btn_cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_cancelActionPerformed(evt);
            }
        });
        getContentPane().add(btn_cancel, new org.netbeans.lib.awtextra.AbsoluteConstraints(950, 650, 300, 40));

        btn_save.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btn_save.setText("SAVE");
        btn_save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_saveActionPerformed(evt);
            }
        });
        getContentPane().add(btn_save, new org.netbeans.lib.awtextra.AbsoluteConstraints(620, 650, 300, 40));

        panel_action_bar.setBackground(new java.awt.Color(153, 153, 153));
        panel_action_bar.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        Pa_btn_kembali.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        Pa_btn_kembali.setText("< kembali");
        panel_action_bar.add(Pa_btn_kembali, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, -1, -1));

        Pa_txt1.setBackground(new java.awt.Color(0, 0, 0));
        Pa_txt1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        Pa_txt1.setForeground(new java.awt.Color(0, 0, 0));
        Pa_txt1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Pa_txt1.setText("HALAMAN KASIR");
        panel_action_bar.add(Pa_txt1, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 10, 1160, 30));

        getContentPane().add(panel_action_bar, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1280, 50));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel2.setText("PELAYAN :");
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 110, 130, 30));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel3.setText("TANGGAL :");
        getContentPane().add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 150, 130, 30));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel7.setText("NAMA PEMBELI :");
        getContentPane().add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 70, 140, 30));

        jLabel20.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel20.setText("STATUS MEMBER :");
        getContentPane().add(jLabel20, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 110, 140, 30));

        jLabel21.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel21.setText("ID MEMBER :");
        getContentPane().add(jLabel21, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 150, 140, 30));
        getContentPane().add(ksr_tanggal, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 150, 210, 30));

        jLabel22.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel22.setText("TOTAL :");
        getContentPane().add(jLabel22, new org.netbeans.lib.awtextra.AbsoluteConstraints(890, 80, 200, 40));

        ksr_tf_total1.setDragEnabled(true);
        ksr_tf_total1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ksr_tf_total1ActionPerformed(evt);
            }
        });
        getContentPane().add(ksr_tf_total1, new org.netbeans.lib.awtextra.AbsoluteConstraints(890, 130, 360, 50));

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel4.setText("SUBTOTAL :");
        getContentPane().add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 470, 140, 30));

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel5.setText("DISKON :");
        getContentPane().add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 510, 140, 30));

        ksr_tf_diskon.setDragEnabled(true);
        ksr_tf_diskon.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ksr_tf_diskonActionPerformed(evt);
            }
        });
        getContentPane().add(ksr_tf_diskon, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 510, 210, 30));

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel6.setText("GRAND TOTAL :");
        getContentPane().add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 590, 140, 30));

        ksr_tf_diskon_member.setDragEnabled(true);
        ksr_tf_diskon_member.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ksr_tf_diskon_memberActionPerformed(evt);
            }
        });
        getContentPane().add(ksr_tf_diskon_member, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 550, 210, 30));

        jLabel23.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel23.setText("DISKON MEMBER :");
        getContentPane().add(jLabel23, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 550, 140, 30));

        ksr_tf_total2.setDragEnabled(true);
        ksr_tf_total2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ksr_tf_total2ActionPerformed(evt);
            }
        });
        getContentPane().add(ksr_tf_total2, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 590, 210, 30));

        jLabel24.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel24.setText("METODE PEMBAYARAN :");
        getContentPane().add(jLabel24, new org.netbeans.lib.awtextra.AbsoluteConstraints(710, 550, 180, 30));

        jLabel25.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel25.setText("BAYAR :");
        getContentPane().add(jLabel25, new org.netbeans.lib.awtextra.AbsoluteConstraints(710, 470, 180, 30));

        jLabel26.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel26.setText("KEMBALIAN :");
        getContentPane().add(jLabel26, new org.netbeans.lib.awtextra.AbsoluteConstraints(710, 510, 180, 30));

        jLabel27.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel27.setText("PEMBAYARAN :");
        getContentPane().add(jLabel27, new org.netbeans.lib.awtextra.AbsoluteConstraints(710, 590, 180, 30));

        jLabel28.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel28.setText("KETERANGAN :");
        getContentPane().add(jLabel28, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 470, 270, 30));

        ksr_tf_id_transaksi.setDragEnabled(true);
        ksr_tf_id_transaksi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ksr_tf_id_transaksiActionPerformed(evt);
            }
        });
        getContentPane().add(ksr_tf_id_transaksi, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 70, 210, 30));

        ksr_tf_pelayan.setDragEnabled(true);
        ksr_tf_pelayan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ksr_tf_pelayanActionPerformed(evt);
            }
        });
        getContentPane().add(ksr_tf_pelayan, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 110, 210, 30));

        ksr_tf_pembeli.setDragEnabled(true);
        ksr_tf_pembeli.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ksr_tf_pembeliActionPerformed(evt);
            }
        });
        getContentPane().add(ksr_tf_pembeli, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 70, 210, 30));

        ksr_tf_subtotal.setDragEnabled(true);
        ksr_tf_subtotal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ksr_tf_subtotalActionPerformed(evt);
            }
        });
        getContentPane().add(ksr_tf_subtotal, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 470, 210, 30));

        ksr_tf_kembalian.setDragEnabled(true);
        ksr_tf_kembalian.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ksr_tf_kembalianActionPerformed(evt);
            }
        });
        getContentPane().add(ksr_tf_kembalian, new org.netbeans.lib.awtextra.AbsoluteConstraints(890, 510, 210, 30));

        ksr_tf_bayar.setDragEnabled(true);
        ksr_tf_bayar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ksr_tf_bayarActionPerformed(evt);
            }
        });
        getContentPane().add(ksr_tf_bayar, new org.netbeans.lib.awtextra.AbsoluteConstraints(890, 470, 210, 30));

        ksr_cb_pembayaran.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "AKTIF", "TIDAK AKTIF" }));
        getContentPane().add(ksr_cb_pembayaran, new org.netbeans.lib.awtextra.AbsoluteConstraints(890, 590, 210, 30));

        ksr_cb_statusmember.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "AKTIF", "TIDAK AKTIF" }));
        getContentPane().add(ksr_cb_statusmember, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 110, 210, 30));

        ksr_cb_metodepembayaran.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "TUNAI", "EWALLET", "DEBIT/KREDIT", "NGUTANG" }));
        ksr_cb_metodepembayaran.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ksr_cb_metodepembayaranActionPerformed(evt);
            }
        });
        getContentPane().add(ksr_cb_metodepembayaran, new org.netbeans.lib.awtextra.AbsoluteConstraints(890, 550, 210, 30));

        ksr_ta_keterangan.setColumns(20);
        ksr_ta_keterangan.setRows(5);
        jScrollPane2.setViewportView(ksr_ta_keterangan);

        getContentPane().add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 510, 270, 110));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void ksr_tf_id_memberActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ksr_tf_id_memberActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_ksr_tf_id_memberActionPerformed

    private void btn_pilih_menuActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_pilih_menuActionPerformed
        // TODO add your handling code here:
        new hal_menu(this).setVisible(true);
    }// GEN-LAST:event_btn_pilih_menuActionPerformed

    private void btn_pilih_inventoryActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_pilih_inventoryActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_btn_pilih_inventoryActionPerformed

    private void btn_editActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_editActionPerformed
        // TODO add your handling code here:
        int selectedRow = tbl_daftarmenu.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih item di tabel yang ingin Anda edit!");
            return;
        }

        DefaultTableModel model = (DefaultTableModel) tbl_daftarmenu.getModel();
        String idMenu = model.getValueAt(selectedRow, 0).toString();
        String namaMenu = model.getValueAt(selectedRow, 1).toString();
        String jumlah = model.getValueAt(selectedRow, 3).toString();

        // --- PERBAIKAN DI SINI ---
        Object keteranganObj = model.getValueAt(selectedRow, 5); // Ambil sebagai Object dulu
        // Periksa jika objeknya null, jika ya, gunakan string kosong. Jika tidak, baru
        // ubah ke string.
        String keterangan = (keteranganObj == null) ? "" : keteranganObj.toString();

        // Panggil constructor baru di hal_menu yang sudah disiapkan untuk edit
        new hal_menu(this, selectedRow, idMenu, namaMenu, jumlah, keterangan).setVisible(true);
    }// GEN-LAST:event_btn_editActionPerformed

    private void btn_cancelActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_cancelActionPerformed
        // TODO add your handling code here:
        ksr_tf_id_transaksi.setText("");
        ksr_tf_pelayan.setText("");
        ksr_tf_pembeli.setText("");
        ksr_tf_id_member.setText("");
        ksr_ta_keterangan.setText("");
        ksr_tf_subtotal.setText("0");
        ksr_tf_diskon.setText("0");
        ksr_tf_diskon_member.setText("0");
        ksr_tf_total1.setText("0");
        ksr_tf_total2.setText("0");
        ksr_tf_bayar.setText("0");
        ksr_tf_kembalian.setText("0");

        // Hapus semua baris dari tabel
        DefaultTableModel model = (DefaultTableModel) tbl_daftarmenu.getModel();
        model.setRowCount(0);

        JOptionPane.showMessageDialog(this, "Transaksi dibatalkan. Form telah dibersihkan.");

    }// GEN-LAST:event_btn_cancelActionPerformed

    private void btn_deleteActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_deleteActionPerformed
        // TODO add your handling code here:
        int selectedRow = tbl_daftarmenu.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih item di tabel yang ingin Anda hapus!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Anda yakin ingin menghapus item ini?", "Konfirmasi Hapus",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            DefaultTableModel model = (DefaultTableModel) tbl_daftarmenu.getModel();
            model.removeRow(selectedRow);
            updateTotals(); // Panggil update total setelah menghapus
            JOptionPane.showMessageDialog(this, "Item berhasil dihapus.");
        }
    }// GEN-LAST:event_btn_deleteActionPerformed

    private void btn_saveActionPerformed(java.awt.event.ActionEvent evt) {
        // Langkah 1: Validasi Input
        if (tbl_daftarmenu.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Belum ada item yang ditambahkan!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (ksr_tanggal.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Tanggal tidak boleh kosong!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Connection conn = null;
        try {
            conn = koneksi.getConnection();
            // Langkah 2: Mulai Transaksi Database
            conn.setAutoCommit(false);

            // Langkah 3: Simpan Data ke Tabel "transaksi"
            String sqlTransaksi = "INSERT INTO transaksi (id_transaksi, nama_pelayan, nama_pembeli, tanggal, " +
                    "status_member, id_member, subtotal, diskon, diskon_member, total, bayar, " +
                    "kembalian, akun_pembayaran, metode_pembayaran, keterangan, jumlah_menu) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstTransaksi = conn.prepareStatement(sqlTransaksi)) {
                int idTransaksi = Integer.parseInt(ksr_tf_id_transaksi.getText());
                pstTransaksi.setInt(1, idTransaksi);

                pstTransaksi.setString(2, ksr_tf_pelayan.getText());
                pstTransaksi.setString(3, ksr_tf_pembeli.getText());

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String tanggal = sdf.format(ksr_tanggal.getDate());
                pstTransaksi.setDate(4, java.sql.Date.valueOf(tanggal));

                String statusMember = ksr_cb_statusmember.getSelectedItem().toString();
                pstTransaksi.setString(5, statusMember);
                if (statusMember.equals("AKTIF")) {
                    pstTransaksi.setString(6, ksr_tf_id_member.getText());
                } else {
                    pstTransaksi.setString(6, "Tidak Aktif");
                }

                long subtotal = Long.parseLong(ksr_tf_subtotal.getText().replace(".", ""));
                long grandTotal = Long.parseLong(ksr_tf_total2.getText().replace(".", ""));
                long bayar = Long.parseLong(ksr_tf_bayar.getText().replace(".", ""));
                long kembalian = Long.parseLong(ksr_tf_kembalian.getText().replace(".", ""));

                pstTransaksi.setLong(7, subtotal);
                pstTransaksi.setString(8, ksr_tf_diskon.getText() + "%");
                pstTransaksi.setString(9, ksr_tf_diskon_member.getText() + "%");
                pstTransaksi.setLong(10, grandTotal);
                pstTransaksi.setLong(11, bayar);
                pstTransaksi.setLong(12, kembalian);

                pstTransaksi.setString(13, ksr_cb_pembayaran.getSelectedItem().toString());
                pstTransaksi.setString(14, ksr_cb_metodepembayaran.getSelectedItem().toString());
                pstTransaksi.setString(15, ksr_ta_keterangan.getText());

                int totalJumlahItem = 0;
                DefaultTableModel model = (DefaultTableModel) tbl_daftarmenu.getModel();
                // Loop setiap baris di tabel
                for (int i = 0; i < model.getRowCount(); i++) {
                    // Ambil nilai dari kolom "Jumlah" (indeks ke-3) dan tambahkan ke total
                    totalJumlahItem += Integer.parseInt(model.getValueAt(i, 3).toString());
                }
                pstTransaksi.setInt(16, totalJumlahItem);

                pstTransaksi.executeUpdate();
            }

            // Langkah 4: Simpan Data ke Tabel "detailTransaksi"
            String sqlDetail = "INSERT INTO \"detailTransaksi\" (id_transaksi, nama_menu, harga_satuan, " +
                    "jumlah_beli, sub_total, keterangan) VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstDetail = conn.prepareStatement(sqlDetail)) {
                DefaultTableModel model = (DefaultTableModel) tbl_daftarmenu.getModel();

                int idTransaksi = Integer.parseInt(ksr_tf_id_transaksi.getText());

                for (int i = 0; i < model.getRowCount(); i++) {
                    pstDetail.setInt(1, idTransaksi);

                    String namaMenu = model.getValueAt(i, 1).toString();
                    long hargaSatuan = Long.parseLong(model.getValueAt(i, 2).toString().replace(".", ""));
                    int jumlahBeli = Integer.parseInt(model.getValueAt(i, 3).toString());
                    long subTotalItem = Long.parseLong(model.getValueAt(i, 4).toString().replace(".", ""));
                    String keteranganItem = model.getValueAt(i, 5).toString();

                    pstDetail.setString(2, namaMenu);
                    pstDetail.setLong(3, hargaSatuan);
                    pstDetail.setInt(4, jumlahBeli);
                    pstDetail.setLong(5, subTotalItem);
                    pstDetail.setString(6, keteranganItem);

                    pstDetail.addBatch();
                }
                pstDetail.executeBatch();
            }

            // Langkah 5: Selesaikan Transaksi dan Pindah Halaman
            conn.commit();
            JOptionPane.showMessageDialog(this, "Transaksi berhasil disimpan!");

            int idTransaksiTersimpan = Integer.parseInt(ksr_tf_id_transaksi.getText());
            new detail_transaksi(idTransaksiTersimpan).setVisible(true);
            this.dispose();

        } catch (SQLException e) {
            // Langkah 6: Batalkan Transaksi Jika Gagal
            JOptionPane.showMessageDialog(this, "Gagal menyimpan transaksi: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Gagal melakukan rollback: " + ex.getMessage());
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Terjadi kesalahan format angka. Periksa kembali inputan.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            // Langkah 7: Kembalikan AutoCommit dan Tutup Koneksi
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Gagal menutup koneksi: " + e.getMessage());
                }
            }
        }
    }// GEN-LAST:event_btn_saveActionPerformed

    private void ksr_tf_total1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ksr_tf_total1ActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_ksr_tf_total1ActionPerformed

    private void ksr_tf_diskonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ksr_tf_diskonActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_ksr_tf_diskonActionPerformed

    private void ksr_tf_diskon_memberActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ksr_tf_diskon_memberActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_ksr_tf_diskon_memberActionPerformed

    private void ksr_tf_total2ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ksr_tf_total2ActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_ksr_tf_total2ActionPerformed

    private void ksr_tf_id_transaksiActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ksr_tf_id_transaksiActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_ksr_tf_id_transaksiActionPerformed

    private void ksr_tf_pelayanActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ksr_tf_pelayanActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_ksr_tf_pelayanActionPerformed

    private void ksr_tf_pembeliActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ksr_tf_pembeliActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_ksr_tf_pembeliActionPerformed

    private void ksr_tf_subtotalActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ksr_tf_subtotalActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_ksr_tf_subtotalActionPerformed

    private void ksr_tf_kembalianActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ksr_tf_kembalianActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_ksr_tf_kembalianActionPerformed

    private void ksr_tf_bayarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ksr_tf_bayarActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_ksr_tf_bayarActionPerformed

    private void ksr_cb_metodepembayaranActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ksr_cb_metodepembayaranActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_ksr_cb_metodepembayaranActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        // <editor-fold defaultstate="collapsed" desc=" Look and feel setting code
        // (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the default
         * look and feel.
         * For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(kasir2.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(kasir2.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(kasir2.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(kasir2.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        // </editor-fold>
        // </editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new kasir2().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel JPanelmenu;
    private javax.swing.JButton Pa_btn_kembali;
    private javax.swing.JLabel Pa_txt1;
    private javax.swing.JButton btn_cancel;
    private javax.swing.JButton btn_delete;
    private javax.swing.JButton btn_edit;
    private javax.swing.JButton btn_pilih_inventory;
    private javax.swing.JButton btn_pilih_menu;
    private javax.swing.JButton btn_save;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JComboBox<String> ksr_cb_metodepembayaran;
    private javax.swing.JComboBox<String> ksr_cb_pembayaran;
    private javax.swing.JComboBox<String> ksr_cb_statusmember;
    private javax.swing.JTextArea ksr_ta_keterangan;
    private com.toedter.calendar.JDateChooser ksr_tanggal;
    private javax.swing.JTextField ksr_tf_bayar;
    private javax.swing.JTextField ksr_tf_diskon;
    private javax.swing.JTextField ksr_tf_diskon_member;
    private javax.swing.JTextField ksr_tf_id_member;
    private javax.swing.JTextField ksr_tf_id_transaksi;
    private javax.swing.JTextField ksr_tf_kembalian;
    private javax.swing.JTextField ksr_tf_pelayan;
    private javax.swing.JTextField ksr_tf_pembeli;
    private javax.swing.JTextField ksr_tf_subtotal;
    private javax.swing.JTextField ksr_tf_total1;
    private javax.swing.JTextField ksr_tf_total2;
    private javax.swing.JPanel panel_action_bar;
    private javax.swing.JTable tbl_daftarmenu;
    // End of variables declaration//GEN-END:variables
}
