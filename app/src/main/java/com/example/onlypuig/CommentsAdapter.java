package com.example.onlypuig;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Map;

import io.appwrite.models.DocumentList;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    // Almacenamos la lista de documentos con los comentarios.
    private DocumentList<Map<String, Object>> commentsList;

    // Metodo para actualizar la lista de comentarios
    public void setComments(DocumentList<Map<String, Object>> commentsList) {
        this.commentsList = commentsList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflamos el layout para cada comentario (asegúrate de tener viewholder_comment.xml en res/layout)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Map<String, Object> commentData = commentsList.getDocuments().get(position).getData();

        // author (si lo guardaste en el doc) o anónimo
        String author = commentData.get("author") != null ? commentData.get("author").toString() : "Anónimo";
        // ahora usamos "content" en vez de "comment"
        String commentText = commentData.get("content") != null ? commentData.get("content").toString() : "";

        holder.authorTextView.setText(author);
        holder.commentTextView.setText(commentText);
    }

    @Override
    public int getItemCount() {
        return commentsList == null ? 0 : commentsList.getDocuments().size();
    }

    // ViewHolder para cada comentario
    static class CommentViewHolder extends RecyclerView.ViewHolder {

        TextView authorTextView;
        TextView commentTextView;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            // Asegúrate de que los IDs coincidan con los definidos en viewholder_comment.xml
            authorTextView = itemView.findViewById(R.id.commentAuthorTextView);
            commentTextView = itemView.findViewById(R.id.commentTextView);
        }
    }
}