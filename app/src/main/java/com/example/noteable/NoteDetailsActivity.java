package com.example.noteable;

//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.EditText;
//import android.widget.ImageButton;
//import android.widget.TextView;
//
//import com.google.android.gms.tasks.OnCompleteListener;
//import com.google.android.gms.tasks.Task;
//import com.google.firebase.Timestamp;
//import com.google.firebase.firestore.DocumentReference;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;



import java.text.BreakIterator;

public class NoteDetailsActivity extends AppCompatActivity {

    EditText titleEditText,contentEditText;
    ImageButton saveNoteBtn;
    TextView pageTitleTextView;
    String title,content,docId;
    boolean isEditMode = false;
    TextView deleteNoteTextViewBtn;
    TextView shareNoteTextViewBtn;

    private static final String CHANNEL_ID = "NoteChannel";
    private static final String CHANNEL_NAME = "NoteNotifications";
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_details);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        titleEditText = findViewById(R.id.notes_title_text);
        contentEditText = findViewById(R.id.notes_content_text);
        saveNoteBtn = findViewById(R.id.save_note_btn);
        pageTitleTextView = findViewById(R.id.page_title);
        deleteNoteTextViewBtn  = findViewById(R.id.delete_note_text_view_btn);
        shareNoteTextViewBtn= findViewById(R.id.share_note_text_view_btn);

        //rec
        title = getIntent().getStringExtra("title");
        content= getIntent().getStringExtra("content");
        docId = getIntent().getStringExtra("docId");

        if(docId!=null && !docId.isEmpty()){
            isEditMode = true;
        }

        titleEditText.setText(title);
        contentEditText.setText(content);

        if(isEditMode){
            pageTitleTextView.setText("Edit your note");
            deleteNoteTextViewBtn.setVisibility(View.VISIBLE);
            shareNoteTextViewBtn.setVisibility(View.VISIBLE);

        }

        saveNoteBtn.setOnClickListener( (v)-> saveNote());
        deleteNoteTextViewBtn.setOnClickListener((v)-> deleteNoteFromFirebase() );

        shareNoteTextViewBtn.setOnClickListener((v)-> shareNote());
    }


    void saveNote() {
        String noteTitle = titleEditText.getText().toString();
        String noteContent = contentEditText.getText().toString();
        if (noteTitle.isEmpty()) {
            titleEditText.setError("Title is required");
            return;
        }

        Note note = new Note();
        note.setTitle(noteTitle);
        note.setContent(noteContent);
        note.setTimestamp(Timestamp.now());

        saveNoteToFirebase(note);

    }

    void saveNoteToFirebase(Note note) {
        DocumentReference documentReference;
        if(isEditMode){
            documentReference = Utility.getCollectionReferenceForNotes().document(docId);
        }else{
            documentReference = Utility.getCollectionReferenceForNotes().document();
        }

        documentReference.set(note).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Utility.showToast(NoteDetailsActivity.this,"Note added successfully");
                    finish();
                }else{
                    Utility.showToast(NoteDetailsActivity.this,"Failed while adding note");
                }
            }
        });
    }

    void deleteNoteFromFirebase(){
        DocumentReference documentReference;
        documentReference = Utility.getCollectionReferenceForNotes().document(docId);
        documentReference.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    //note is deleted
                    Utility.showToast(NoteDetailsActivity.this,"Note deleted successfully");
                    finish();
                }else{
                    Utility.showToast(NoteDetailsActivity.this,"Failed while deleting note");
                }
            }
        });
    }

    void shareNote() {
        shareNoteTextViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String title = titleEditText.getText().toString();
                String content = contentEditText.getText().toString();

                String noteTitle = titleEditText.getText().toString();
                String noteContent = contentEditText.getText().toString();


                String shareText = "Title: " + title + "\n\n" + "Content: "+ content;

                if (noteTitle == null || noteTitle.isEmpty()) {
                    titleEditText.setError("Title is required");
                    return;
                }

                Note note = new Note();
                note.setTitle(noteTitle);
                note.setContent(noteContent);
                note.setTimestamp(Timestamp.now());

                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                shareIntent.setType("text/plain");

                startActivity(Intent.createChooser(shareIntent, "Share Via"));

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendLocalNotification("Note Shared Successfully");
                    }
                }, 8000);

            }
        });
    }

    private void sendLocalNotification(String message) {
        NotificationCompat.Builder builder = getChannelNotification("Noteable App", message);
        Intent resultIntent = new Intent(this, NoteDetailsActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(resultPendingIntent);
        notificationManager.notify(1, builder.build());
    }


    public NotificationCompat.Builder getChannelNotification(String title, String message) {
        createNotificationChannel();

        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setAutoCancel(true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}