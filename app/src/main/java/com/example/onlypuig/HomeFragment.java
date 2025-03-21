package com.example.onlypuig;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;


import io.appwrite.Client;
import io.appwrite.Query;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;

public class HomeFragment extends Fragment {
    NavController navController;

    ImageView photoImageView;
    TextView displayNameTextView, emailTextView;

    Client client;
    Account account;
    String userId;
    PostsAdapter adapter;
    AppViewModel appViewModel;

    public HomeFragment() {
    }

    void deletePost(String postId) {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            databases.deleteDocument(
                    getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                    postId,
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al eliminar el post: " + error.toString(), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        // Refrescar lista de posts despues de eliminar
                        mainHandler.post(() -> obtenerPosts());
                    })
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        NavigationView navigationView = view.getRootView().findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        photoImageView = header.findViewById(R.id.imageView);
        displayNameTextView = header.findViewById(R.id.displayNameTextView);
        emailTextView = header.findViewById(R.id.emailTextView);
        client = new Client(requireContext()).setProject(getString(R.string.APPWRITE_PROJECT_ID));
        account = new Account(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            account.get(new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }
                mainHandler.post(() -> {
                    userId = result.getId();
                    displayNameTextView.setText(result.getName().toString());
                    emailTextView.setText(result.getEmail().toString());
                    Glide.with(requireView()).load(R.drawable.user).into(photoImageView);
                    obtenerPosts();
                });
            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.newPostFragment);
            }
        });
        RecyclerView postsRecyclerView = view.findViewById(R.id.postsRecyclerView);
        adapter = new PostsAdapter();
        postsRecyclerView.setAdapter(adapter);

        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView authorPhotoImageView, likeImageView, mediaImageView, deleteImageView;
        TextView authorTextView, contentTextView, numLikesTextView, commentTextView, dateTextView;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            authorPhotoImageView = itemView.findViewById(R.id.authorPhotoImageView);
            likeImageView = itemView.findViewById(R.id.likeImageView);
            mediaImageView = itemView.findViewById(R.id.mediaImage);
            deleteImageView = itemView.findViewById(R.id.deleteImageView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
            commentTextView = itemView.findViewById(R.id.commentTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
        }
    }

    class PostsAdapter extends RecyclerView.Adapter<PostViewHolder> {
        DocumentList<Map<String, Object>> lista = null;

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_post, parent, false);
            return new PostViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {

            // Obtener el documento completo
            Document<Map<String, Object>> document = lista.getDocuments().get(position);
            Map<String, Object> post = document.getData();

            holder.authorTextView.setText(post.get("author").toString());
            holder.contentTextView.setText(post.get("content").toString());

            // Configuración de la imagen del autor
            if (post.get("uid") != null) {
                loadAuthorProfilePhoto(post.get("uid").toString(), holder.authorPhotoImageView);
            } else {
                holder.authorPhotoImageView.setImageResource(R.drawable.user);
            }

            // Formatear y mostrar la fecha de publicación
            String createdAt = document.getCreatedAt(); // Se asume que retorna un String en formato ISO8601, ej. "2021-10-20T14:30:00.000Z"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                holder.dateTextView.setText(TimeUtils.getRelativeTime(createdAt));
            }

            // Gestión de likes (código existente)
            List<String> likes = (List<String>) post.get("likes");
            if (likes.contains(userId))
                holder.likeImageView.setImageResource(R.drawable.like_on);
            else
                holder.likeImageView.setImageResource(R.drawable.like_off);
            holder.numLikesTextView.setText(String.valueOf(likes.size()));
            holder.likeImageView.setOnClickListener(view -> {
                Databases databases = new Databases(client);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                List<String> nuevosLikes = likes;
                if (nuevosLikes.contains(userId))
                    nuevosLikes.remove(userId);
                else
                    nuevosLikes.add(userId);
                Map<String, Object> data = new HashMap<>();
                data.put("likes", nuevosLikes);
                try {
                    databases.updateDocument(
                            getString(R.string.APPWRITE_DATABASE_ID),
                            getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                            post.get("$id").toString(),
                            data,
                            new ArrayList<>(),
                            new CoroutineCallback<>((result, error) -> {
                                if (error != null) {
                                    error.printStackTrace();
                                    return;
                                }
                                mainHandler.post(() -> obtenerPosts());
                            })
                    );
                } catch (AppwriteException e) {
                    e.printStackTrace();
                }
            });

            // Gestión de la miniatura de media (código existente)
            if (post.get("mediaUrl") != null) {
                holder.mediaImageView.setVisibility(View.VISIBLE);
                if ("audio".equals(post.get("mediaType").toString())) {
                    Glide.with(requireView()).load(R.drawable.audio).centerCrop().into(holder.mediaImageView);
                } else {
                    Glide.with(getContext()).load(post.get("mediaUrl").toString()).centerCrop().into(holder.mediaImageView);
                }
                holder.mediaImageView.setOnClickListener(view -> {
                    appViewModel.postSeleccionado.setValue(post);
                    navController.navigate(R.id.mediaFragment);
                });
            } else {
                holder.mediaImageView.setVisibility(View.GONE);
            }

            // Dentro de onBindViewHolder de PostsAdapter:
            holder.commentTextView.setOnClickListener(view -> {
                // Guardamos el post actual en el ViewModel para que el fragmento de comentarios lo reciba
                appViewModel.postSeleccionado.setValue(post);
                // Navegamos al fragmento de comentarios (asegúrate de tener definido el id en tu nav_graph)
                navController.navigate(R.id.commentsRecyclerView);
            });

            // Mostrar botón de eliminar solo si el usuario es el autor del post
            if (post.get("uid") != null && post.get("uid").toString().equals(userId)) {
                holder.deleteImageView.setVisibility(View.VISIBLE);
                holder.deleteImageView.setOnClickListener(view -> {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Eliminar post")
                            .setMessage("¿Estás seguro de que deseas eliminar este post?")
                            .setPositiveButton("Sí", (dialog, which) -> {
                                deletePost(post.get("$id").toString());
                            })
                            .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                            .show();
                });
            } else {
                holder.deleteImageView.setVisibility(View.GONE);
            }
        }

        private String getRelativeTime(String createdAt) {
            try {
                // Instant.parse() interpreta directamente el string en formato ISO-8601.
                Instant created = Instant.parse(createdAt);
                Duration duration = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    duration = Duration.between(created, Instant.now());
                }
                long seconds = duration.getSeconds();
                if (seconds < 60) {
                    return "Publicado hace " + seconds + " seg";
                } else if (seconds < 3600) {
                    long minutes = seconds / 60;
                    return "Publicado hace " + minutes + " min";
                } else if (seconds < 86400) {
                    long hours = seconds / 3600;
                    return "Publicado hace " + hours + " h";
                } else {
                    long days = seconds / 86400;
                    return "Publicado hace " + days + (days > 1 ? " dias" : " dia");
                }
            } catch (DateTimeParseException e) {
                e.printStackTrace();
                // Si falla, retorna el string original (o podrías retornar otro mensaje de error)
                return createdAt;
            }
        }

        @Override
        public int getItemCount() {
            return lista == null ? 0 : lista.getDocuments().size();
        }

        public void establecerLista(DocumentList<Map<String, Object>> lista) {
            this.lista = lista;
            notifyDataSetChanged();
        }
        private void loadAuthorProfilePhoto(String uid, ImageView imageView) {
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
                                String url = result.getDocuments().get(0)
                                        .getData().get("profilePhotoUrl").toString();
                                new Handler(Looper.getMainLooper()).post(() ->
                                        Glide.with(imageView.getContext())
                                                .load(url)
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

    void obtenerPosts() {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            databases.listDocuments(getString(R.string.APPWRITE_DATABASE_ID), // databaseId
                    getString(R.string.APPWRITE_POSTS_COLLECTION_ID), // collectionId
                    new ArrayList<>(), // queries (optional)
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al obtener los posts: " + error.toString(), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        System.out.println(result.toString());
                        mainHandler.post(() -> adapter.establecerLista(result));
                    }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }
}