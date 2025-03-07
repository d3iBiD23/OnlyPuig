package com.example.onlypuig;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.services.Databases;

public class CommentsFragment extends Fragment {
    private RecyclerView commentsRecyclerView;
    private EditText commentEditText;
    private Button postCommentButton;
    private CommentsAdapter adapter;
    private AppViewModel appViewModel;
    private Client client;
    private String postId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        commentsRecyclerView = view.findViewById(R.id.commentsRecyclerView);
        commentEditText = view.findViewById(R.id.commentEditText);
        postCommentButton = view.findViewById(R.id.postCommentButton);

        // Obtenemos el post seleccionado (su id) desde el ViewModel o argumentos
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        Map<String, Object> selectedPost = appViewModel.postSeleccionado.getValue();
        if (selectedPost != null && selectedPost.get("$id") != null) {
            postId = selectedPost.get("$id").toString();
        }

        client = new Client(requireContext()).setProject(getString(R.string.APPWRITE_PROJECT_ID));

        loadComments();

        postCommentButton.setOnClickListener(v -> {
            String commentText = commentEditText.getText().toString();
            if (TextUtils.isEmpty(commentText)) {
                commentEditText.setError("Requerido");
                return;
            }
            postComment(commentText);
        });
    }

    private void loadComments() {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            // Aquí debes filtrar por "postId" para obtener solo los comentarios del post
            databases.listDocuments(
                    getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_COMMENTS_COLLECTION_ID),
                    new ArrayList<String>() {{
                        add("equal('postId', '" + postId + "')");
                    }},
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al cargar comentarios: " + error.toString(), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        mainHandler.post(() -> adapter.setComments(result));
                    })
            );
        } catch (AppwriteException e) {
            e.printStackTrace();
        }
    }

    private void postComment(String commentText) {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Map<String, Object> data = new HashMap<>();
        data.put("postId", postId);
        data.put("content", commentText);
        // Puedes agregar más campos, como "author" o "createdAt"
        try {
            databases.createDocument(
                    getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_COMMENTS_COLLECTION_ID),
                    "unique()",
                    data,
                    new ArrayList<>(),
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al publicar comentario: " + error.toString(), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        mainHandler.post(() -> {
                            commentEditText.setText("");
                            loadComments();
                        });
                    })
            );
        } catch (AppwriteException e) {
            e.printStackTrace();
        }
    }
}
