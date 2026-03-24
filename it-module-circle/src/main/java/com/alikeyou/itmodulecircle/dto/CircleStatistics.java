package com.alikeyou.itmodulecircle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CircleStatistics {

    private Long totalCircles;

    private Long totalMembers;

    private Long activeMembers;

    private Long totalPosts;


}
