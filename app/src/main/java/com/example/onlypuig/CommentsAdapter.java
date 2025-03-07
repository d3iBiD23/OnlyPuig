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
        // Obtenemos los datos del comentario
        Map<String, Object> commentData = commentsList.getDocuments().get(position).getData();

        // Extraemos campos, por ejemplo "author" y "comment"
        String author = commentData.get("author") != null ? commentData.get("author").toString() : "Anónimo";
        String commentText = commentData.get("comment") != null ? commentData.get("comment").toString() : "";

        // Asignamos los valores a las vistas
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