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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Asumiendo que en fragment_comments.xml tienes un RecyclerView con id "commentsRecyclerView",
        // un EditText con id "commentEditText" y un Button con id "postCommentButton".
        commentsRecyclerView = view.findViewById(R.id.commentsRecyclerView);
        commentEditText = view.findViewById(R.id.commentEditText);
        postCommentButton = view.findViewById(R.id.postCommentButton);

        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CommentsAdapter();
        commentsRecyclerView.setAdapter(adapter);

        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        // El post seleccionado lo guardamos en el ViewModel
        Map<String, Object> post = appViewModel.postSeleccionado.getValue();
        if (post != null && post.get("$id") != null) {
            postId = post.get("$id").toString();
        }
        client = new Client(requireContext()).setProject(getString(R.string.APPWRITE_PROJECT_ID));

        // Cargar los comentarios (directamente del post)
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

    // Lee el arreglo "comments" del post actual y lo asigna al adapter
    private void loadComments() {
        Map<String, Object> post = appViewModel.postSeleccionado.getValue();
        ArrayList<Map<String, Object>> comments = new ArrayList<>();
        if (post != null && post.get("comments") != null) {
            comments = (ArrayList<Map<String, Object>>) post.get("comments");
        }
        adapter.setComments(comments);
    }

    // Agrega un comentario al arreglo "comments" y actualiza el documento del post
    private void postComment(String commentText) {
        Map<String, Object> post = appViewModel.postSeleccionado.getValue();
        if (post == null) return;
        ArrayList<Map<String, Object>> comments;
        if (post.get("comments") != null) {
            comments = (ArrayList<Map<String, Object>>) post.get("comments");
        } else {
            comments = new ArrayList<>();
        }
        // Crea el nuevo comentario (aquí puedes usar datos reales del usuario si los tienes)
        HashMap<String, Object> newComment = new HashMap<>();
        newComment.put("author", "Anónimo"); // O userName si lo tienes
        newComment.put("content", commentText);
        newComment.put("createdAt", System.currentTimeMillis());
        comments.add(newComment);

        // Prepara el mapa de datos a actualizar
        Map<String, Object> data = new HashMap<>();
        data.put("comments", comments);

        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            // Actualiza el documento del post (su id es postId)
            databases.updateDocument(
                    getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                    postId,
                    data,
                    new ArrayList<>(),
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            mainHandler.post(() ->
                                    Snackbar.make(requireView(), "Error al actualizar comentarios: " + error.toString(), Snackbar.LENGTH_LONG).show()
                            );
                            return;
                        }
                        // Actualiza el post en el ViewModel con los datos retornados
                        appViewModel.postSeleccionado.setValue(result.getData());
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