import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class BeatBox {
    JFrame frame;
   JPanel mainPanel;
   JList incomingList;
   JTextField userMessage;
   int nextNum;
   Vector<String> listVector = new Vector<String>();
   String userName;
   ObjectOutputStream out;
   ObjectInputStream in;
   ArrayList<JCheckBox> checkboxList;
   HashMap<String, boolean[]> otherSeqsMap = new HashMap<String,boolean[]>();
   Sequencer sequencer;
   Sequence sequence;
   Sequence mySequence = null;
   Track track;


   String [] instrumentsNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal",
   "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom",
   "Hi Agogo", "Open Hi Conga"};
   int[] instruments = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};

    public static void main(String[] args) {
       new BeatBox().startUp("%javaBeatBox theFlash");
    }

    public void startUp(String name){
        userName = name;

        try{
            Socket socket = new Socket("127.0.0.1",4242);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Thread remote = new Thread(new RemoteReader());
            remote.start();
        }catch (Exception e){
            e.printStackTrace();
        }
        setUpMidi();
        buildGUI();
    }

    public void buildGUI(){
        frame = new JFrame("Digital BeatBox");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        checkboxList = new ArrayList<JCheckBox>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        JButton buttonStart = new JButton("Start");
        buttonStart.addActionListener(new MyStartListener());
        buttonBox.add(buttonStart);

        JButton buttonStop = new JButton("Stop");
        buttonStop.addActionListener(new MyStopListener());
        buttonBox.add(buttonStop);

        JButton buttonTempoUp = new JButton("Tempo Up");
        buttonTempoUp.addActionListener(new MyUpTempoListener());
        buttonBox.add(buttonTempoUp);

        JButton buttonTempoDown = new JButton("Tempo Down");
        buttonTempoDown.addActionListener(new MyDownTempoListener());
        buttonBox.add(buttonTempoDown);

      /*  JButton buttonSerializelt = new JButton("Serializelt");
        buttonSerializelt.addActionListener(new SaveMenuListener());
        buttonBox.add(buttonSerializelt);

        JButton buttonRestore = new JButton("Restore");
        buttonRestore.addActionListener(new OpenMenuListener());
        buttonBox.add(buttonRestore); */

        JButton buttonSendIt = new JButton("Send it");
        buttonSendIt.addActionListener(new MySendListener());
        buttonBox.add(buttonSendIt);

        userMessage = new JTextField();
        buttonBox.add(userMessage);

        incomingList = new JList();
        incomingList.addListSelectionListener(new MyListSelectionListener());
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        buttonBox.add(theList);
        incomingList.setListData(listVector);

        /*
        JMenuBar menuBarSave = new JMenuBar();
        JMenu fileMenuSave = new JMenu("File");
        JMenuItem saveMenuItem = new JMenuItem("Save");
        saveMenuItem.addActionListener(new SaveMenuListener());
        fileMenuSave.add(saveMenuItem);
        menuBarSave.add(fileMenuSave);
        frame.setJMenuBar(menuBarSave); */

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < 16; i++) {
            nameBox.add(new Label(instrumentsNames[i]));
        }

        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST,nameBox);

        frame.getContentPane().add(background);

        GridLayout grid = new GridLayout(16,16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER,mainPanel);


        /*
        JMenuBar menuBarLoad = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem loadMenuItem = new JMenuItem("Load Beatbox");
        loadMenuItem.addActionListener(new OpenMenuListener());
        fileMenu.add(loadMenuItem);
        menuBarLoad.add(fileMenu);
        frame.setJMenuBar(menuBarLoad); */

        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkboxList.add(c);
            mainPanel.add(c);
        }
        setUpMidi();

        frame.setBounds(50,50,300,300);
        frame.pack();
        frame.setVisible(true);
    }

    public void setUpMidi (){
        try{
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ,4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        }catch(Exception ex){
            ex.getStackTrace();
        }
    }

    public void buildTrackAndStart(){
        ArrayList<Integer> trackList = null;

        sequence.deleteTrack(track);  // удаляем старую
        track = sequence.createTrack();  // создаем новую

        for (int i = 0; i < 16; i++) {
            trackList = new ArrayList<Integer>();

            for (int j = 0; j < 16; j++) {
                JCheckBox jc = (JCheckBox) checkboxList.get(j + (16*i));
                if(jc.isSelected()){
                    int key = instruments[i];
                    trackList.add(key);
                } else{
                    trackList.add(null);
                }
            }
            makeTracks(trackList);
               // track.add(makeEvent(176,1,127,0,16));
        }
        track.add(makeEvent(192,9,1,0,55));
        try{
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public class MyStartListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            buildTrackAndStart();
        }
    }

    public class MyStopListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            sequencer.stop();
        }
    }

    public class MyUpTempoListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            float tempoFactory = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactory * 1.03));
        }
    }

    public class MyDownTempoListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            float tempoFactory = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactory * 0.97));
        }
    }

    public class MySendListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean[] checkboxState = new boolean[256];
            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                if (check.isSelected()){
                    checkboxState[i] = true;
                }
            }
            String messageToSend = null;
            try{
                out.writeObject(userName + nextNum++ +": " + userMessage.getText());
                out.writeObject(checkboxState);
            }catch(Exception el){
                System.out.println("Sorry dude. Could not send it to the server.");
            }
            userMessage.setText("");
        }
    }


    public class MyListSelectionListener implements ListSelectionListener{
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if(e.getValueIsAdjusting()){
                String selected = (String) incomingList.getSelectedValue();
                if (selected != null){
                    boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
                    changeSequence(selectedState);
                    sequencer.stop();
                    buildTrackAndStart();
                }
            }
        }
    }

    public class RemoteReader implements Runnable{
        boolean[] checkboxState = null;
        String nameToShow = null;
        Object obj = null;

        @Override
        public void run() {
            try{
                while((obj= in.readObject()) != null){
                    System.out.println("got an object from server");
                    System.out.println(obj.getClass());
                    String nameToShow = (String) obj;
                    checkboxState = (boolean[]) in.readObject();
                    otherSeqsMap.put(nameToShow,checkboxState);
                    listVector.add(nameToShow);
                    incomingList.setListData(listVector);
                }
            }catch (Exception ew){
                ew.printStackTrace();
            }
        }
    }

    public class MyPlayMineListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            if(mySequence != null){
                sequence = mySequence;
            }
        }
    }

    public void changeSequence(boolean[] checkboxState){
        for (int i = 0; i < 256; i++) {
            JCheckBox check = (JCheckBox) checkboxList.get(i);
            if (checkboxState[i]){
                check.setSelected(true);
            } else{
                check.setSelected(false);
            }
        }
    }

    public void makeTracks(ArrayList list){
        Iterator it = list.iterator();
        for (int i = 0; i < 16; i++) {
            Integer num = (Integer) it.next();
            if (num != null){
                int numKey = num.intValue();
                track.add(makeEvent(144,9,numKey,100,i));
                track.add(makeEvent(128,9,numKey,100,i+1));
            }
        }
    }

    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick){
        MidiEvent event = null;
        try {
          ShortMessage shortMessage = new ShortMessage();
          shortMessage.setMessage(comd,chan,one,two);
          event = new MidiEvent(shortMessage,tick);
        } catch (Exception e){
            e.printStackTrace();
        }
        return event;
    }

/*
    public class MySendListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean[] checkboxState = new boolean[256];
            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                if (check.isSelected()){
                    checkboxState[i] = true;
                }
            }

            try{
                ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream("Checkbox.ser"));
                os.writeObject(checkboxState);
            } catch (Exception w){
                w.printStackTrace();
            }
        }
    } */

    public class SaveMenuListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileSave = new JFileChooser();
            fileSave.showSaveDialog(frame);
            saveFile(fileSave.getSelectedFile());
        }
    }

    private void saveFile(File file){
        boolean[] checkboxState = new boolean[256];
        for (int i = 0; i < 256; i++) {
            JCheckBox check = (JCheckBox) checkboxList.get(i);
            if (check.isSelected()){
                checkboxState[i] = true;
            }
        }

        try{
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file));
            os.writeObject(checkboxState);
        } catch (Exception w){
            w.printStackTrace();
        }
    }
    public class OpenMenuListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileOpen = new JFileChooser();
            fileOpen.showOpenDialog(frame);
            loadFile(fileOpen.getSelectedFile());
        }
    }

    private void loadFile(File file){
        boolean[] checkboxState = null;
        try{
            ObjectInputStream is = new ObjectInputStream(new FileInputStream(file));
            checkboxState = (boolean[]) is.readObject();
        } catch (Exception q){
            q.printStackTrace();
        }
        for (int i = 0; i < 256; i++) {
            JCheckBox check = (JCheckBox) checkboxList.get(i);
            if (checkboxState[i]){
                check.setSelected(true);
            } else{
                check.setSelected(false);
            }
        }

        sequencer.stop();
        buildTrackAndStart();
    }


}
