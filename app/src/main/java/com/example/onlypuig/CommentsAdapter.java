package com.example.onlypuig;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.Query;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.DocumentList;
import io.appwrite.services.Databases;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    public interface OnCommentDeleteListener {
        void onDeleteComment(Map<String, Object> comment);
    }
    private List<Map<String, Object>> comments;
    private String currentUserName;
    private OnCommentDeleteListener deleteListener;
    private Client client; // Necesario para las consultas a la base de datos

    public CommentsAdapter(Client client) {
        this.client = client;
    }

    public void setComments(List<Map<String, Object>> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    public void setCurrentUserName(String currentUserName) {
        this.currentUserName = currentUserName;
    }

    public void setOnCommentDeleteListener(OnCommentDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Map<String, Object> comment = comments.get(position);
        String author = comment.get("author") != null ? comment.get("author").toString() : "Anónimo";
        String content = comment.get("content") != null ? comment.get("content").toString() : "";
        holder.authorTextView.setText(author);
        holder.commentTextView.setText(content);

        // Cargar la foto del autor del comentario
        if(comment.containsKey("uid")){
            String uid = comment.get("uid").toString();
            loadAuthorProfilePhoto(uid, holder.commentProfileImageView);
        } else {
            holder.commentProfileImageView.setImageResource(R.drawable.user);
        }

        // Mostrar "Borrar comentario" solo si el comentario pertenece al usuario actual
        if (currentUserName != null && currentUserName.equals(author)) {
            holder.deleteCommentTextView.setVisibility(View.VISIBLE);
            holder.deleteCommentTextView.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteComment(comment);
                }
            });
        } else {
            holder.deleteCommentTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return comments == null ? 0 : comments.size();
    }

    // Metodo para cargar la foto de perfil de un usuario según su uid (similar al de PostsAdapter)
    private void loadAuthorProfilePhoto(String uid, ImageView imageView) {
        Databases databases = new Databases(client);
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("uid", uid));
        try {
            databases.listDocuments(
                    imageView.getContext().getString(R.string.APPWRITE_DATABASE_ID),
                    imageView.getContext().getString(R.string.APPWRITE_PROFILE_COLLECTION_ID),
                    queries,
                    new CoroutineCallback<>((result, error) -> {
                        String url = null;
                        if (error == null && !result.getDocuments().isEmpty()) {
                            Object urlObj = result.getDocuments().get(0).getData().get("profilePhotoUrl");
                            if (urlObj != null && !urlObj.toString().isEmpty()) {
                                url = urlObj.toString();
                            }
                        }
                        final String finalUrl = url;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (finalUrl != null) {
                                Glide.with(imageView.getContext())
                                        .load(finalUrl)
                                        .circleCrop()
                                        .into(imageView);
                            } else {
                                Glide.with(imageView.getContext())
                                        .load(R.drawable.user)
                                        .circleCrop()
                                        .into(imageView);
                            }
                        });
                    })
            );
        } catch (AppwriteException e) {
            e.printStackTrace();
            new Handler(Looper.getMainLooper()).post(() ->
                    Glide.with(imageView.getContext())
                            .load(R.drawable.user)
                            .circleCrop()
                            .into(imageView)
            );
        }
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView commentProfileImageView;
        TextView authorTextView, commentTextView, deleteCommentTextView;
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            commentProfileImageView = itemView.findViewById(R.id.commentProfileImageView);
            authorTextView = itemView.findViewById(R.id.commentAuthorTextView);
            commentTextView = itemView.findViewById(R.id.commentTextView);
            deleteCommentTextView = itemView.findViewById(R.id.deleteCommentTextView);
        }
    }
}