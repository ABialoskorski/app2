package com.example.artur.app2;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SecondActivity extends AppCompatActivity {

    private static final android.util.Base64 Base64 = null;
    private TextView mheadinglabel;
    private ImageView mfingerprintimage;
    private TextView mparalabel;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private TextView profileNote2;
    String outputString;
    String AES = "AES";


    private FingerprintManager fingerprintManager;
    private KeyguardManager keyguardManager;

    private KeyStore keyStore;
    private Cipher cipher;

    FingerprintHandler fingerprintHandler = new FingerprintHandler(this);
    private String KEY_NAME = "AndroidKey";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        profileNote2 = findViewById(R.id.tvProfileNote2);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        DatabaseReference databaseReference = firebaseDatabase.getReference(firebaseAuth.getUid());

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                profileNote2.setText("Note: " + userProfile.getUserNote());

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SecondActivity.this, databaseError.getCode(), Toast.LENGTH_SHORT).show();
            }
        });
/*
        private void Logout () {
            firebaseAuth.signOut();
            finish();
            startActivity(new Intent(SecondActivity.this, MainActivity.class));

        }


        @Override
        public boolean onCreateOptionsMenu (Menu menu){
            getMenuInflater().inflate(R.menu.menu, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected (MenuItem item){

            switch (item.getItemId()) {
                case R.id.logoutMenu: {
                    Logout();
                }
                case R.id.profileMenu:
                    startActivity(new Intent(SecondActivity.this, ProfileActivity.class));
            }
            return super.onOptionsItemSelected(item);
        }
*/

        mheadinglabel = (TextView) findViewById(R.id.headinglabel);
        mfingerprintimage = (ImageView) findViewById(R.id.fingerprintimage);
        mparalabel = (TextView) findViewById(R.id.paralabel);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
            keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

            if (!fingerprintManager.isHardwareDetected()) {
                mparalabel.setText("Fingerprint scanner is not in this Device!");
            } else if (!keyguardManager.isKeyguardSecure()) {
                mparalabel.setText("Secure your lock screen");
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                mparalabel.setText("Don't have access to use fingerprint");
            } else if (!fingerprintManager.hasEnrolledFingerprints()) {
                mparalabel.setText("You should at least 1 fingerprint to use this feature");
            } else {
                mparalabel.setText("Place your finger on scanner");

                generateKey();

                if (cipherInit()) {
                    FingerprintManager.CryptoObject cryptoObject = new FingerprintManager.CryptoObject(cipher);
                    FingerprintHandler fingerprintHandler = new FingerprintHandler(this);
                    fingerprintHandler.startAuth(fingerprintManager, cryptoObject);
                }
            }
        }


    }



    @TargetApi(Build.VERSION_CODES.M)
    private void generateKey() {

        try {

            keyStore = KeyStore.getInstance("AndroidKeyStore");
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            keyStore.load(null);
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();

        } catch (KeyStoreException | IOException | CertificateException
                | NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | NoSuchProviderException e) {

            e.printStackTrace();

        }

    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean cipherInit() {
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }

        try {

            keyStore.load(null);

            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);

            cipher.init(Cipher.ENCRYPT_MODE, key);

            return true;

        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }

    }
}