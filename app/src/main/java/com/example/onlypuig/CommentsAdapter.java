package com.example.onlypuig;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import io.appwrite.models.DocumentList;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Map<String, Object>> comments;

    public void setComments(List<Map<String, Object>> comments) {
        this.comments = comments;
        notifyDataSetChanged();
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
    }

    @Override
    public int getItemCount() {
        return comments == null ? 0 : comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView authorTextView, commentTextView;
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextView = itemView.findViewById(R.id.commentAuthorTextView);
            commentTextView = itemView.findViewById(R.id.commentTextView);
        }
    }
}