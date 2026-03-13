package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmoduleinteractive.entiey.Comment;
import com.alikeyou.itmoduleinteractive.entiey.LikeRecord;
import com.alikeyou.itmoduleinteractive.repository.CommentRepository;
import com.alikeyou.itmoduleinteractive.repository.LikeRecordRepository;
import com.alikeyou.itmoduleinteractive.service.LikeRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LikeRecordServiceImpl implements LikeRecordService {

    @Autowired
    private LikeRecordRepository likeRecordRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Override
    public List<LikeRecord> getAllLikeRecords() {
        return likeRecordRepository.findAll();
    }

    @Override
    public Optional<LikeRecord> getLikeRecordById(Long id) {
        return likeRecordRepository.findById(id);
    }

    @Override
    public LikeRecord createLikeRecord(LikeRecord likeRecord) {
        LikeRecord savedLikeRecord = likeRecordRepository.save(likeRecord);
        // 如果目标类型是评论，更新评论的点赞数
        if ("comment".equals(likeRecord.getTargetType())) {
            updateCommentLikes(likeRecord.getTargetId());
        }
        return savedLikeRecord;
    }

    @Override
    public void deleteLikeRecord(Long id) {
        Optional<LikeRecord> likeRecordOptional = likeRecordRepository.findById(id);
        if (likeRecordOptional.isPresent()) {
            LikeRecord likeRecord = likeRecordOptional.get();
            likeRecordRepository.deleteById(id);
            // 如果目标类型是评论，更新评论的点赞数
            if ("comment".equals(likeRecord.getTargetType())) {
                updateCommentLikes(likeRecord.getTargetId());
            }
        }
    }

    @Override
    public Optional<LikeRecord> getLikeRecordByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId) {
        return likeRecordRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
    }

    @Override
    public List<LikeRecord> getLikeRecordsByTargetTypeAndTargetId(String targetType, Long targetId) {
        return likeRecordRepository.findByTargetTypeAndTargetId(targetType, targetId);
    }

    @Override
    public List<LikeRecord> getLikeRecordsByUserId(Long userId) {
        return likeRecordRepository.findByUserId(userId);
    }

    /**
     * 更新评论的点赞数
     * @param commentId 评论ID
     */
    private void updateCommentLikes(Long commentId) {
        Optional<Comment> commentOptional = commentRepository.findById(commentId);
        if (commentOptional.isPresent()) {
            Comment comment = commentOptional.get();
            // 计算该评论的点赞数
            List<LikeRecord> likeRecords = likeRecordRepository.findByTargetTypeAndTargetId("comment", commentId);
            comment.setLikes(likeRecords.size());
            commentRepository.save(comment);
        }
    }
}