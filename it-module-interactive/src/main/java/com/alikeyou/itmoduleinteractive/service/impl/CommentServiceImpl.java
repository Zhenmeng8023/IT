package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmoduleinteractive.entity.Comment;
import com.alikeyou.itmoduleinteractive.entity.LikeRecord;
import com.alikeyou.itmoduleinteractive.repository.CommentRepository;
import com.alikeyou.itmoduleinteractive.repository.LikeRecordRepository;
import com.alikeyou.itmoduleinteractive.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private LikeRecordRepository likeRecordRepository;

    @Override
    public List<Comment> getAllComments() {
        List<Comment> comments = commentRepository.findAll();
        // 确保每个评论的点赞数是最新的
        comments.forEach(this::updateCommentLikes);
        return comments;
    }

    @Override
    public Optional<Comment> getCommentById(Long id) {
        Optional<Comment> commentOptional = commentRepository.findById(id);
        commentOptional.ifPresent(this::updateCommentLikes);
        return commentOptional;
    }

    @Override
    public Comment saveComment(Comment comment) {
        // 新评论初始化点赞数为0
        if (comment.getLikes() == null) {
            comment.setLikes(0);
        }
        return commentRepository.save(comment);
    }

    @Override
    public List<Comment> getCommentsByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        // 确保每个评论的点赞数是最新的
        comments.forEach(this::updateCommentLikes);
        return comments;
    }

    /**
     * 更新评论的点赞数
     * @param comment 评论对象
     */
    private void updateCommentLikes(Comment comment) {
        // 计算该评论的点赞数
        List<LikeRecord> likeRecords = likeRecordRepository.findByTargetTypeAndTargetId("comment", comment.getId());
        comment.setLikes(likeRecords.size());
        // 保存更新后的点赞数
        commentRepository.save(comment);
    }
}