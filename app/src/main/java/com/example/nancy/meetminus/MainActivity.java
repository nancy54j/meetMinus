package com.example.nancy.meetminus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nancy.meetminus.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.left.rightmesh.android.AndroidMeshManager;
import io.left.rightmesh.android.MeshService;
import io.left.rightmesh.id.MeshID;
import io.left.rightmesh.mesh.MeshManager;
import io.left.rightmesh.mesh.MeshStateListener;
import io.left.rightmesh.util.MeshUtility;
import io.left.rightmesh.util.RightMeshException;
import io.reactivex.functions.Consumer;

import static android.app.PendingIntent.getActivity;
import static io.left.rightmesh.mesh.MeshManager.DATA_RECEIVED;
import static io.left.rightmesh.mesh.MeshManager.PEER_CHANGED;
import static io.left.rightmesh.mesh.MeshManager.REMOVED;
import static java.lang.Math.sqrt;

public class MainActivity extends AppCompatActivity implements MeshStateListener{

    //set proximity

    // Port to bind app to.
    private static final int HELLO_PORT = 1000;

    private DatabaseReference myRef;

    // MeshManager instance - interface to the mesh network.
    AndroidMeshManager mm = null;
    private User me;
    private Map<User, String> friends;
    // Set to keep track of peers connected to the mesh.
    Set<User> users = new HashSet<>();

    /**
     * Called when app first opens, initializes {@link AndroidMeshManager} reference (which will
     * start the {@link MeshService} if it isn't already running.
     *
     * @param savedInstanceState passed from operating system
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mm = AndroidMeshManager.getInstance(MainActivity.this, MainActivity.this);
        Intent i = getIntent();
        me = (User) i.getSerializableExtra("MeUser");
        me.setMeshID(mm.getUuid());

        friends = me.getFriends();
    }

    //function that constantly updates longitude and latitude_____________

    /**function that loops through your contacts and checks your friends locations
     *if within a certain proximity to you, send alert
    **/
    public void findNearbyFriends(){

        for (Map.Entry<User, String> entry : friends.entrySet())
        {
           User user = entry.getKey();
           double lat = user.getLatitude();
           double lon = user.getLongitude();

           double dist = sqrt((me.getLatitude() - lat)*(me.getLatitude() - lat)+ (me.getLongitude()-lon)*(me.getLongitude()-lon));

            System.out.println(entry.getKey() + "/" + entry.getValue());
        }

    }


    /**
     * Called when activity is on screen.
     */
    @Override
    protected void onResume() {
        try {
            super.onResume();
            mm.resume();
        } catch (MeshService.ServiceDisconnectedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the app is being closed (not just navigated away from). Shuts down
     * the {@link AndroidMeshManager} instance.
     */
    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            mm.stop();
        } catch (MeshService.ServiceDisconnectedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Called by the {@link MeshService} when the mesh state changes. Initializes mesh connection
     * on first call.
     *
     * @param uuid our own user id on first detecting
     * @param state state which indicates SUCCESS or an error code
     */
    @Override
    public void meshStateChanged(MeshID uuid, int state) {
        if (state == MeshStateListener.SUCCESS) {
            try {
                // Binds this app to MESH_PORT.
                // This app will now receive all events generated on that port.
                mm.bind(HELLO_PORT);

                // Subscribes handlers to receive events from the mesh.
                mm.on(DATA_RECEIVED, new Consumer() {
                    @Override
                    public void accept(Object o) throws Exception {
                        handleDataReceived((MeshManager.RightMeshEvent) o);
                    }
                });
                mm.on(PEER_CHANGED, new Consumer() {
                    @Override
                    public void accept(Object o) throws Exception {
                        handlePeerChanged((MeshManager.RightMeshEvent) o);
                    }
                });

                // If you are using Java 8 or a lambda backport like RetroLambda, you can use
                // a more concise syntax, like the following:
                // mm.on(PEER_CHANGED, this::handlePeerChanged);
                // mm.on(DATA_RECEIVED, this::dataReceived);

                // Enable buttons now that mesh is connected.
                String status = "Connected";

                final EditText edTxtFriend = (EditText) findViewById(R.id.edTxtFriend);

                Button btnConnect = (Button) findViewById(R.id.btnConnect);
                btnConnect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            //connect to a friend
                            connect(view, edTxtFriend.getText().toString());
                        } catch (RightMeshException e) {
                            e.printStackTrace();
                        }
                    }
                });

                TextView textAdd = (TextView) findViewById(R.id.textAdd);
                TextView textStatus = (TextView) findViewById(R.id.textStatus);

                //enables views
                btnConnect.setEnabled(true);
                edTxtFriend.setEnabled(true);
                textAdd.setEnabled(true);

                //notify status
                textStatus.setText(status);

            } catch (RightMeshException e) {
                String status = "Error initializing the library" + e.toString();
                Toast.makeText(getApplicationContext(), status, Toast.LENGTH_SHORT).show();
                TextView textStatus = (TextView) findViewById(R.id.textStatus);
                textStatus.setText(status);
                return;
            }
        }

        // Update display on successful calls (i.e. not FAILURE or DISABLED).
        if (state == MeshStateListener.SUCCESS || state == MeshStateListener.RESUME) {
            updateStatus();
        }
    }

    /**
     * Update the {@link TextView} with a list of all peers.
     */
    private void updateStatus() {
        String status = "uuid: " + mm.getUuid().toString() + "\npeers:\n";
        for (User user : users) {
            status += user.getUsername() + "\n";
        }
        TextView txtStatus = (TextView) findViewById(R.id.textStatus);
        txtStatus.setText(status);
    }

    /**
     * Handles incoming data events from the mesh - toasts the contents of the data.
     *
     * @param e event object from mesh
     */
    private void handleDataReceived(MeshManager.RightMeshEvent e) {
        final MeshManager.DataReceivedEvent event = (MeshManager.DataReceivedEvent) e;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final String userID = new String(event.data);
                final String Category = "friend";

                myRef = FirebaseDatabase.getInstance().getReference();

                myRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User friend = findUser(dataSnapshot, userID);
                        me.addFriend(friend, Category);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });






                //make a dialog_____________

                // Play a notification.
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(MainActivity.this, notification);
                r.play();
            }
        });
    }

    public User findUser(DataSnapshot dataSnapshot, String userID){
       return dataSnapshot.child(userID).getValue(User.class);
    }

    public Dialog onCreateDialog(String userID) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        String message = userID+"wants to connect";
        String pos = "Connect";
        String neg = "Ignore";
        builder.setMessage(message)
                .setPositiveButton(pos, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // add contact
                    }
                })
                .setNegativeButton(neg, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // close
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
    /**
     * Add ppl who are online to your group!!
     * Handles peer update events from the mesh - maintains a list of peers and updates the display.
     *
     * @param e event object from mesh
     */
    //????????????????how to add object???????????????
    private void handlePeerChanged(MeshManager.RightMeshEvent e) {
        // Update peer list.
        MeshManager.PeerChangedEvent event = (MeshManager.PeerChangedEvent) e;
        if (event.state != REMOVED && !users.contains(event.peerUuid)) {
            users.add(event.peerUuid);
        } else if (event.state == REMOVED){
            users.remove(event.peerUuid);
        }

        // Update display.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateStatus();
            }
        });
    }

    /**
     * Send your userID to person you want to connect.
     *
     * @param v calling view
     */
    public void connect(View v, String friendUsername) throws RightMeshException {
        for(User receiver : users) {
            if (friendUsername.equals(receiver.getUsername())) {
                //send userID
                mm.sendDataReliable(receiver.getMeshID(), HELLO_PORT, me.userID.getBytes());

            }
        }
    }

    /**
     * Open mesh settings screen.
     *
     * @param v calling view
     */
    public void configure(View v)
    {
        try {
            mm.showSettingsActivity();
        } catch(RightMeshException ex) {
            MeshUtility.Log(this.getClass().getCanonicalName(), "Service not connected");
        }
    }

}
