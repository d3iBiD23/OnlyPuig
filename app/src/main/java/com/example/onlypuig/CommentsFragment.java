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
import androidx.recyclerview.widget.LinearLayoutManager;
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
    private String userId;      // Para guardar el UID
    private String userName;    // Para guardar el nombre del autor

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        commentsRecyclerView = view.findViewById(R.id.commentsFragment);
        commentEditText = view.findViewById(R.id.commentEditText);
        postCommentButton = view.findViewById(R.id.postCommentButton);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CommentsAdapter();
        commentsRecyclerView.setAdapter(adapter);

        // Obtenemos el post seleccionado (su id) desde el ViewModel
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        Map<String, Object> selectedPost = appViewModel.postSeleccionado.getValue();
        if (selectedPost != null && selectedPost.get("$id") != null) {
            postId = selectedPost.get("$id").toString();
        }

        client = new Client(requireContext()).setProject(getString(R.string.APPWRITE_PROJECT_ID));

        // (Opcional) Cargar el usuario actual si quieres guardar su nombre/uid en el comentario
        // Podrías hacerlo igual que en HomeFragment, con account.get(...)
        // Suponiendo que ya lo obtuviste antes, por ejemplo:
        // userId = ...
        // userName = ...

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
            // Filtrar por "postId"
            ArrayList<String> queries = new ArrayList<>();
            queries.add("equal('postId','" + postId + "')");

            databases.listDocuments(
                    getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_COMMENTS_COLLECTION_ID),
                    queries,
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            mainHandler.post(() ->
                                    Snackbar.make(requireView(), "Error al cargar comentarios: " + error.toString(), Snackbar.LENGTH_LONG).show()
                            );
                            return;
                        }
                        // Muy importante: setear la lista en el adapter
                        mainHandler.post(() -> {
                            adapter.setComments(result);
                        });
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

        try {
            databases.createDocument(
                    getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_COMMENTS_COLLECTION_ID),
                    "unique()",
                    data,
                    new ArrayList<>(),
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            mainHandler.post(() ->
                                    Snackbar.make(requireView(), "Error al publicar comentario: " + error.toString(), Snackbar.LENGTH_LONG).show()
                            );
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
