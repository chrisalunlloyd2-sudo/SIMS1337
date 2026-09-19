package com.aigen.sims.gui;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AegisSwingSphere extends JFrame {
    private SpherePanel spherePanel;
    private JTextArea ganStream;
    private JTextArea probeStream;
    private JLabel sectorStatus;
    private JProgressBar gpuGauge;
    private JProgressBar vramGauge;

    public AegisSwingSphere() {
        setTitle("AEGIS OTG - Omniscient Execution Engine (Machine 2 Node)");
        setSize(1200, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));
        getContentPane().setBackground(new Color(15, 15, 15));

        // ==========================================
        // CENTER: The 3D Living Sphere Mesh
        // ==========================================
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(10, 10, 10));
        spherePanel = new SpherePanel();
        centerPanel.add(spherePanel, BorderLayout.CENTER);
        
        // Sector Status Ring below sphere
        sectorStatus = new JLabel("Sector Status: [ CORE ONLINE - WAITING FOR TELEMETRY ]", SwingConstants.CENTER);
        sectorStatus.setForeground(Color.GREEN);
        sectorStatus.setFont(new Font("Consolas", Font.BOLD, 14));
        centerPanel.add(sectorStatus, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        // ==========================================
        // LEFT PANEL: Karoo GP & Diagnostics HUD
        // ==========================================
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(new Color(20, 20, 20));
        leftPanel.setPreferredSize(new Dimension(320, 0));

        // 1. Evolutionary Controls
        JPanel evoPanel = createTitledPanel("Evolutionary & Karoo GP");
        evoPanel.add(createSlider("Population Dynamics", 10, 1000, 500));
        JButton shockBtn = new JButton("FORCE EPOCH / SHOCK");
        shockBtn.setBackground(new Color(200, 100, 0));
        shockBtn.setForeground(Color.WHITE);
        evoPanel.add(shockBtn);
        JCheckBox symSimp = new JCheckBox("Symbolic Simplifier (SymPy)");
        symSimp.setForeground(Color.CYAN);
        symSimp.setOpaque(false);
        evoPanel.add(symSimp);
        leftPanel.add(evoPanel);

        // 2. Health & Sector Diagnostics
        JPanel diagPanel = createTitledPanel("System Health & Diagnostics");
        gpuGauge = new JProgressBar(0, 100);
        vramGauge = new JProgressBar(0, 100);
        diagPanel.add(createLiveGauge("GPU Compute Load", gpuGauge));
        diagPanel.add(createLiveGauge("VRAM Allocation", vramGauge));
        diagPanel.add(createSlider("Thermal & OOM Safety Ceiling", 50, 100, 90));
        JCheckBox matrixCheck = new JCheckBox("Route Emergency to CLI Queue");
        matrixCheck.setForeground(Color.WHITE);
        matrixCheck.setOpaque(false);
        diagPanel.add(matrixCheck);
        leftPanel.add(diagPanel);

        // 3. Automated Search & Diagnostics HUD
        JPanel searchPanel = createTitledPanel("Automated Search HUD");
        searchPanel.add(createSlider("Search Threshold (Aggressiveness)", 0, 100, 75));
        probeStream = new JTextArea(4, 20);
        probeStream.setBackground(Color.BLACK);
        probeStream.setForeground(new Color(255, 150, 0));
        probeStream.setFont(new Font("Consolas", Font.PLAIN, 11));
        probeStream.setText("> Probe initialized...\n> Searching DuckDuckGo for: OOM fix\n> Context injected to local LLM.");
        searchPanel.add(new JScrollPane(probeStream));
        leftPanel.add(searchPanel);

        add(leftPanel, BorderLayout.WEST);

        // ==========================================
        // RIGHT PANEL: GAN Overlay & Compute Sharing
        // ==========================================
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(new Color(20, 20, 20));
        rightPanel.setPreferredSize(new Dimension(350, 0));

        // 1. Compute & Memory Sharing Pane
        JPanel sharePanel = createTitledPanel("Compute & Memory Sharing");
        sharePanel.add(createSlider("Lend to Machine 1 (The Mint)", 0, 100, 20));
        JCheckBox rayDask = new JCheckBox("Enable ZeroMQ / Plasma Bridge");
        rayDask.setForeground(Color.GREEN);
        rayDask.setOpaque(false);
        rayDask.setSelected(true);
        sharePanel.add(rayDask);
        rightPanel.add(sharePanel);

        // 2. Additive Network Controls
        JPanel netPanel = createTitledPanel("Additive Network Controls");
        netPanel.add(createSlider("Novelty / Exploration Dial (Temp)", 0, 100, 50));
        JCheckBox guardrails = new JCheckBox("Strict Additive Guardrails");
        guardrails.setForeground(Color.CYAN);
        guardrails.setOpaque(false);
        guardrails.setSelected(true);
        netPanel.add(guardrails);
        
        // Pipeline Radios
        JLabel pipeLbl = new JLabel("Injection Pipeline:");
        pipeLbl.setForeground(Color.WHITE);
        netPanel.add(pipeLbl);
        ButtonGroup pipeGroup = new ButtonGroup();
        JRadioButton r1 = new JRadioButton("Direct to Karoo Pool");
        JRadioButton r2 = new JRadioButton("CLI Review Queue");
        r1.setOpaque(false); r1.setForeground(Color.WHITE);
        r2.setOpaque(false); r2.setForeground(Color.WHITE); r2.setSelected(true);
        pipeGroup.add(r1); pipeGroup.add(r2);
        netPanel.add(r1); netPanel.add(r2);
        rightPanel.add(netPanel);

        // 3. Generative Proposal (GAN) Stream
        JPanel ganPanel = createTitledPanel("Generative Proposal Stream");
        ganStream = new JTextArea(8, 25);
        ganStream.setBackground(Color.BLACK);
        ganStream.setForeground(new Color(0, 210, 255));
        ganStream.setFont(new Font("Consolas", Font.PLAIN, 12));
        ganStream.setText("[ Candidate #402 ] Predicted Gain: +14.2%\n\"Suggesting tensor vectorized sub-expression\"\n\n[ Candidate #403 ] Building AST...\n> Qwen2.5 Mutator active.");
        ganPanel.add(new JScrollPane(ganStream));
        
        JPanel ganBtns = new JPanel(new FlowLayout());
        ganBtns.setOpaque(false);
        ganBtns.add(new JButton("Accept & Inject"));
        ganBtns.add(new JButton("Queue for Test"));
        ganPanel.add(ganBtns);
        rightPanel.add(ganPanel);

        add(rightPanel, BorderLayout.EAST);

        // ==========================================
        // BOTTOM PANEL: Alerts & Mute
        // ==========================================
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(Color.BLACK);
        JCheckBox muteBtn = new JCheckBox("Emergency Mute / Override");
        muteBtn.setForeground(Color.RED);
        muteBtn.setOpaque(false);
        bottomPanel.add(muteBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // Animation Timer for 3D Sphere Rotation
        Timer timer = new Timer(30, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                spherePanel.angleX += 0.015;
                spherePanel.angleY += 0.025;
                spherePanel.repaint();
            }
        });
        timer.start();
        
        shockBtn.addActionListener(e -> {
            sectorStatus.setForeground(Color.ORANGE);
            sectorStatus.setText("Sector Status: [ AMBER - FITNESS PLATEAU SHOCK INJECTED ]");
            ganStream.append("\n\n[ALERT] Temperature spike initiated by User.");
        });

        // Start Background Telemetry Listener
        startUDPListener();
    }

    private void startUDPListener() {
        Thread listener = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(5556)) {
                byte[] buffer = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    
                    // Basic Regex parsing of string telemetry for speed
                    // Expected format: GPU:85,VRAM:40,COLOR:RED,MSG:Crash
                    SwingUtilities.invokeLater(() -> {
                        if (msg.contains("GPU:")) {
                            Matcher m = Pattern.compile("GPU:(\\d+)").matcher(msg);
                            if (m.find()) {
                                int val = Integer.parseInt(m.group(1));
                                gpuGauge.setValue(val);
                                gpuGauge.setForeground(val > 80 ? Color.RED : Color.GREEN);
                            }
                        }
                        if (msg.contains("VRAM:")) {
                            Matcher m = Pattern.compile("VRAM:(\\d+)").matcher(msg);
                            if (m.find()) {
                                int val = Integer.parseInt(m.group(1));
                                vramGauge.setValue(val);
                                vramGauge.setForeground(val > 80 ? Color.RED : Color.GREEN);
                            }
                        }
                        if (msg.contains("STATUS:")) {
                            Matcher m = Pattern.compile("STATUS:([^,]+)").matcher(msg);
                            if (m.find()) {
                                String stat = m.group(1);
                                sectorStatus.setText("Sector Status: " + stat);
                                if (stat.contains("CRITICAL") || stat.contains("OOM")) {
                                    sectorStatus.setForeground(Color.RED);
                                    spherePanel.coreColor = new Color(255, 0, 0, 130);
                                } else if (stat.contains("SHOCK")) {
                                    sectorStatus.setForeground(Color.ORANGE);
                                    spherePanel.coreColor = new Color(255, 150, 0, 130);
                                } else {
                                    sectorStatus.setForeground(Color.GREEN);
                                    spherePanel.coreColor = new Color(0, 210, 255, 130);
                                }
                            }
                        }
                        if (msg.contains("GAN:")) {
                            Matcher m = Pattern.compile("GAN:([^,]+)").matcher(msg);
                            if (m.find()) {
                                ganStream.append("\n" + m.group(1));
                                ganStream.setCaretPosition(ganStream.getDocument().getLength());
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        listener.setDaemon(true);
        listener.start();
    }

    private JPanel createTitledPanel(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(25, 25, 25));
        TitledBorder tb = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(50, 50, 50)), title);
        tb.setTitleColor(new Color(0, 210, 255));
        p.setBorder(tb);
        return p;
    }

    private JPanel createSlider(String label, int min, int max, int val) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setForeground(Color.LIGHT_GRAY);
        JSlider s = new JSlider(min, max, val);
        s.setOpaque(false);
        s.setForeground(Color.CYAN);
        p.add(l, BorderLayout.NORTH);
        p.add(s, BorderLayout.CENTER);
        return p;
    }
    
    private JPanel createLiveGauge(String label, JProgressBar pb) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setForeground(Color.LIGHT_GRAY);
        pb.setForeground(Color.GREEN);
        pb.setBackground(Color.DARK_GRAY);
        p.add(l, BorderLayout.NORTH);
        p.add(pb, BorderLayout.CENTER);
        return p;
    }

    class SpherePanel extends JPanel {
        double angleX = 0, angleY = 0;
        int radius = 180;
        int nodes = 24;
        Color coreColor = new Color(0, 210, 255, 130);

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Deep space gradient
            GradientPaint gp = new GradientPaint(0, 0, new Color(5, 5, 10), 0, getHeight(), new Color(20, 20, 30));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            g2.setColor(coreColor); // Dynamic Core Color
            
            for (int i = 0; i <= nodes; i++) {
                double lat0 = Math.PI * (-0.5 + (double) (i - 1) / nodes);
                double z0 = Math.sin(lat0);
                double zr0 = Math.cos(lat0);

                double lat1 = Math.PI * (-0.5 + (double) i / nodes);
                double z1 = Math.sin(lat1);
                double zr1 = Math.cos(lat1);

                for (int j = 0; j <= nodes; j++) {
                    double lng = 2 * Math.PI * (double) (j - 1) / nodes;
                    double x = Math.cos(lng);
                    double y = Math.sin(lng);
                    
                    double px1 = x * zr0, py1 = y * zr0, pz1 = z0;
                    double rx1 = px1 * Math.cos(angleY) - pz1 * Math.sin(angleY);
                    double rz1 = px1 * Math.sin(angleY) + pz1 * Math.cos(angleY);
                    double ry1 = py1 * Math.cos(angleX) - rz1 * Math.sin(angleX);
                    
                    double px2 = x * zr1, py2 = y * zr1, pz2 = z1;
                    double rx2 = px2 * Math.cos(angleY) - pz2 * Math.sin(angleY);
                    double rz2 = px2 * Math.sin(angleY) + pz2 * Math.cos(angleY);
                    double ry2 = py2 * Math.cos(angleX) - rz2 * Math.sin(angleX);

                    int drawX1 = cx + (int)(rx1 * radius);
                    int drawY1 = cy + (int)(ry1 * radius);
                    int drawX2 = cx + (int)(rx2 * radius);
                    int drawY2 = cy + (int)(ry2 * radius);

                    g2.drawLine(drawX1, drawY1, drawX2, drawY2);
                    
                    // Surface Particles
                    if (j % 4 == 0) {
                        g2.setColor(Color.WHITE);
                        g2.fillOval(drawX1 - 2, drawY1 - 2, 4, 4);
                        g2.setColor(coreColor);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AegisSwingSphere().setVisible(true);
        });
    }
}
