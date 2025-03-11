package com.example.onlypuig;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.Query;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.InputFile;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;
import io.appwrite.services.Storage;

public class ProfileFragment extends Fragment {
    NavController navController;
    ImageView photoImageView;
    TextView displayNameTextView, emailTextView;

    Client client;
    Account account;
    Storage storage;
    String userId;

    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        navController = Navigation.findNavController(view);
        photoImageView = view.findViewById(R.id.photoImageView);
        displayNameTextView = view.findViewById(R.id.displayNameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);

        client = new Client(requireContext()).setProject(getString(R.string.APPWRITE_PROJECT_ID));
        account = new Account(client);
        storage = new Storage(client);

        // Configurar el launcher para seleccionar imagen
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                uploadProfilePhoto(uri);
            }
        });

        // Permitir cambiar la foto pulsando sobre ella
        photoImageView.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Cargar los datos del usuario
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            account.get(new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }
                userId = result.getId();
                mainHandler.post(() -> {
                    displayNameTextView.setText(result.getName().toString());
                    emailTextView.setText(result.getEmail().toString());
                    // Se carga la foto actualizada del perfil desde la base de datos
                    loadProfilePhoto(userId, photoImageView);
                });
            }));
        } catch (AppwriteException e) {
            e.printStackTrace();
        }
    }

    private void uploadProfilePhoto(Uri uri) {
        try {
            File file = getFileFromUri(requireContext(), uri);
            // Sube el archivo al bucket de perfil (el bucket ID debe estar definido en tus recursos, p.ej. APPWRITE_PROFILE_BUCKET_ID)
            storage.createFile(getString(R.string.APPWRITE_PROFILE_BUCKET), "unique()",
                    InputFile.Companion.fromFile(file), new ArrayList<>(),
                    new CoroutineCallback<>((result, error) -> {
                        if(error != null) {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    Toast.makeText(requireContext(), "Error al subir foto: " + error.getMessage(), Toast.LENGTH_LONG).show());
                            return;
                        }
                        // Construir la URL de descarga (ajusta el endpoint según tu configuración)
                        String downloadUrl = "https://cloud.appwrite.io/v1/storage/buckets/"
                                + getString(R.string.APPWRITE_PROFILE_BUCKET)
                                + "/files/" + result.getId()
                                + "/view?project=" + getString(R.string.APPWRITE_PROJECT_ID)
                                + "&mode=admin";
                        // Actualiza (o crea) el documento del perfil
                        updateUserProfilePhoto(downloadUrl);
                        new Handler(Looper.getMainLooper()).post(() ->
                                Glide.with(requireView()).load(downloadUrl).into(photoImageView));
                    }));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // Metodo para actualizar o crear el documento de perfil en la colección "profiles"
    private void updateUserProfilePhoto(String photoUrl) {
        Databases databases = new Databases(client);
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("uid", userId));
        try {
            databases.listDocuments(getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_PROFILE_COLLECTION_ID),
                    queries, new CoroutineCallback<>((result, error) -> {
                        if(error != null || result.getDocuments().isEmpty()){
                            // Si no existe el documento, lo creamos
                            createProfileDocument(photoUrl);
                        } else {
                            // Si existe, actualizamos el documento existente
                            String profileDocId = result.getDocuments().get(0).getId();
                            Map<String, Object> data = new HashMap<>();
                            data.put("profilePhotoUrl", photoUrl);
                            try {
                                databases.updateDocument(getString(R.string.APPWRITE_DATABASE_ID),
                                        getString(R.string.APPWRITE_PROFILE_COLLECTION_ID),
                                        profileDocId, data, new ArrayList<>(),
                                        new CoroutineCallback<>((updResult, updError) -> {
                                            if(updError != null) {
                                                // Manejar error de actualización
                                            }
                                        }));
                            } catch(AppwriteException e) {
                                e.printStackTrace();
                            }
                        }
                    }));
        } catch(AppwriteException e) {
            e.printStackTrace();
        }
    }
    private void createProfileDocument(String photoUrl) {
        Databases databases = new Databases(client);
        Map<String, Object> data = new HashMap<>();
        data.put("uid", userId);
        data.put("profilePhotoUrl", photoUrl);
        try {
            databases.createDocument(getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_PROFILE_COLLECTION_ID),
                    "unique()", data, new ArrayList<>(),
                    new CoroutineCallback<>((result, error) -> {
                        if(error != null) {
                            // Manejar error de creación
                        }
                    }));
        } catch(AppwriteException e) {
            e.printStackTrace();
        }
    }

    // Metodo auxiliar similar al que ya tienes en NewPostFragment
    public File getFileFromUri(Context context, Uri uri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if(inputStream == null)
            throw new FileNotFoundException("No se pudo abrir el URI: " + uri);
        String fileName = "profile_photo.jpg";
        File tempFile = new File(context.getCacheDir(), fileName);
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        byte[] buffer = new byte[1024];
        int length;
        while((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        outputStream.close();
        inputStream.close();
        return tempFile;
    }
    private void loadProfilePhoto(String uid, ImageView imageView) {
        Databases databases = new Databases(client);
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("uid", uid));
        try {
            databases.listDocuments(
                    getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_PROFILE_COLLECTION_ID),
                    queries,
                    new CoroutineCallback<>((result, error) -> {
                        if (error == null && !result.getDocuments().isEmpty()) {
                            String profilePhotoUrl = result.getDocuments().get(0)
                                    .getData().get("profilePhotoUrl").toString();
                            new Handler(Looper.getMainLooper()).post(() ->
                                    Glide.with(imageView.getContext())
                                            .load(profilePhotoUrl)
                                            .circleCrop()
                                            .into(imageView));
                        }
                    })
            );
        } catch (AppwriteException e) {
            e.printStackTrace();
        }
    }

}