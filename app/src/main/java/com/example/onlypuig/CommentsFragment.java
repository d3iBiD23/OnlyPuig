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
import java.util.List;
import java.util.Map;
import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;
import io.appwrite.Query;

public class CommentsFragment extends Fragment {
    private RecyclerView commentsRecyclerView;
    private EditText commentEditText;
    private Button postCommentButton;
    private CommentsAdapter adapter;
    private AppViewModel appViewModel;
    private Client client;
    private String postId;
    private String currentUserName;

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

        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CommentsAdapter();
        commentsRecyclerView.setAdapter(adapter);

        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        // El post seleccionado lo guardamos en el ViewModel
        Map<String, Object> post = appViewModel.postSeleccionado.getValue();

        if (post != null && post.get("$id") != null) {
            postId = post.get("$id").toString();
            System.out.println("postId: " + postId);
        } else {
            System.out.println("El post seleccionado es nulo o no contiene $id");
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

        // Obtén el usuario actual
        Account account = new Account(client);
        try {
            account.get(new CoroutineCallback<>((userResult, userError) -> {
                if (userError != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Snackbar.make(requireView(), "Error al obtener usuario: " + userError.getMessage(), Snackbar.LENGTH_LONG).show());
                    return;
                }
                currentUserName = userResult.getName().toString();
                new Handler(Looper.getMainLooper()).post(() -> adapter.setCurrentUserName(currentUserName));
            }));
        } catch (AppwriteException e) {
            e.printStackTrace();
        }

        // Configura el callback para borrar un comentario
        adapter.setOnCommentDeleteListener(comment -> {
            String commentId = comment.get("$id").toString();
            deleteComment(commentId);
        });
    }

    private void deleteComment(String commentId) {
        Databases databases = new Databases(client);
        databases.deleteDocument(
                getString(R.string.APPWRITE_DATABASE_ID),
                getString(R.string.APPWRITE_COMMENTS_COLLECTION_ID),
                commentId,
                new CoroutineCallback<>((result, error) -> {
                    if (error != null) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                Snackbar.make(requireView(), "Error al eliminar comentario: " + error.getMessage(), Snackbar.LENGTH_LONG).show());
                        return;
                    }
                    // Recarga los comentarios tras la eliminación
                    new Handler(Looper.getMainLooper()).post(() -> loadComments());
                })
        );
    }
    // Lee el arreglo "comments" del post actual y lo asigna al adapter
    private void loadComments() {
        Databases databases = new Databases(client);
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("postId", postId));  // O usa la cadena si ya está indexado
        try {
            databases.listDocuments(
                    getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_COMMENTS_COLLECTION_ID),
                    queries,
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    Snackbar.make(requireView(), "Error al cargar comentario: " + error.getMessage(), Snackbar.LENGTH_LONG).show()
                            );
                            return;
                        }
                        ArrayList<Map<String, Object>> comments = new ArrayList<>();
                        for (var doc : result.getDocuments()) {
                            Map<String, Object> originalData = doc.getData();
                            Map<String, Object> minimalComment = new HashMap<>();
                            minimalComment.put("$id", doc.getId()); // Agrega el ID del comentario
                            minimalComment.put("author", originalData.get("author"));
                            minimalComment.put("content", originalData.get("content"));
                            minimalComment.put("createdAt", originalData.get("createdAt"));
                            comments.add(minimalComment);
                        }
                        new Handler(Looper.getMainLooper()).post(() -> {
                            adapter.setComments(comments);
                        });
                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    // Agrega un comentario al arreglo "comments" y actualiza el documento del post
    private void postComment(String commentText) {
        // Primero, obtenemos los datos del usuario actual
        Account account = new Account(client);
        try {
            account.get(new CoroutineCallback<>((userResult, userError) -> {
                if (userError != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Snackbar.make(requireView(), "Error al obtener usuario: " + userError.getMessage(), Snackbar.LENGTH_LONG).show()
                    );
                    return;
                }
                String userName = userResult.getName().toString();

                // Preparamos el nuevo comentario usando el nombre real del usuario
                Map<String, Object> newComment = new HashMap<>();
                newComment.put("postId", postId);
                newComment.put("author", userName);
                newComment.put("content", commentText);
                newComment.put("createdAt", String.valueOf(System.currentTimeMillis() / 1000));

                Databases databases = new Databases(client);
                try {
                    databases.createDocument(
                            getString(R.string.APPWRITE_DATABASE_ID),
                            getString(R.string.APPWRITE_COMMENTS_COLLECTION_ID),
                            "unique()", // Para que Appwrite genere el ID
                            newComment,
                            new ArrayList<>(),
                            new CoroutineCallback<>((result, error) -> {
                                if (error != null) {
                                    new Handler(Looper.getMainLooper()).post(() ->
                                            Snackbar.make(requireView(), "Error al crear comentario: " + error.getMessage(), Snackbar.LENGTH_LONG).show()
                                    );
                                    return;
                                }
                                // Vuelve a cargar los comentarios después de publicar
                                loadComments();
                                // Limpia el EditText
                                new Handler(Looper.getMainLooper()).post(() -> commentEditText.setText(""));
                            })
                    );
                } catch (AppwriteException e) {
                    throw new RuntimeException(e);
                }
            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }
}