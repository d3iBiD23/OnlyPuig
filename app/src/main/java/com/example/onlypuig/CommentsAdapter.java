package com.example.onlypuig;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import io.appwrite.models.DocumentList;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    public interface OnCommentDeleteListener {
        void onDeleteComment(Map<String, Object> comment);
    }
    private List<Map<String, Object>> comments;
    private String currentUserName;
    private OnCommentDeleteListener deleteListener;

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

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView authorTextView, commentTextView, deleteCommentTextView;
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextView = itemView.findViewById(R.id.commentAuthorTextView);
            commentTextView = itemView.findViewById(R.id.commentTextView);
            deleteCommentTextView = itemView.findViewById(R.id.deleteCommentTextView);
        }
    }
}