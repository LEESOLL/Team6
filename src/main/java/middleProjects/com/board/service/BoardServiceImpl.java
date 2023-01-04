package middleProjects.com.board.service;

import lombok.RequiredArgsConstructor;
import middleProjects.com.board.dto.*;
import middleProjects.com.board.entity.Board;
import middleProjects.com.board.entity.BoardRecommendation;
import middleProjects.com.board.repository.BoardRecommendationRepository;
import middleProjects.com.board.repository.BoardRepository;
import middleProjects.com.comment.dto.CommentResponseDto;
import middleProjects.com.comment.entity.Comment;
import middleProjects.com.comment.repository.CommentRecommendationRepository;
import middleProjects.com.comment.repository.CommentRepository;
import middleProjects.com.member.entity.Member;
import middleProjects.com.member.repository.MemberRepository;
import middleProjects.com.security.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;
    private final CommentRecommendationRepository commentRecommendationRepository;
    private final BoardRecommendationRepository boardRecommendationRepository;

    //게시글 생성
    @Transactional
    public CreateBoardResponseDto createBoard(CreateBoardRequestDto createBoardRequestDto) {
        String user = SecurityUtil.getCurrentMemberEmail();
        Member member = memberRepository.findByUsername(user).orElseThrow(IllegalArgumentException::new);
        Board board = new Board(createBoardRequestDto.getTitle(), createBoardRequestDto.getContent(), member);
        boardRepository.save(board);
        return new CreateBoardResponseDto(board);
    }

    //게시물 전체 조회
    @Transactional
    public List<RetrieveBoardResponseDto> retrieveBoardList() {
        Page<Board> boardPage = boardRepository.findAll(pageableSetting());
        List<RetrieveBoardResponseDto> retrieveBoardResponseDtoList = new ArrayList<>();
        for (Board board : boardPage) {
            Page<Comment> commentPage = commentRepository.findAllByBoard(board, pageableSetting());
            List<CommentResponseDto> commentList = new ArrayList<>();
            for (Comment comment : commentPage) {
                Long commentRecommendCount = commentRecommendationRepository.countByComment(comment);
                commentList.add(new CommentResponseDto(comment, commentRecommendCount));
            }
            retrieveBoardResponseDtoList.add(new RetrieveBoardResponseDto(board, 0L, commentList));
        }
        return retrieveBoardResponseDtoList;
    }


    //게시물 하나 조회
    @Transactional
    public RetrieveBoardResponseDto retrieveBoard(Long boardId) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        Page<Comment> commentPage = commentRepository.findAllByBoard(board, pageableSetting());
        List<CommentResponseDto> commentList = new ArrayList<>();
        for (Comment comment : commentPage) {
            Long commentRecommendCount = commentRecommendationRepository.countByComment(comment);
            commentList.add(new CommentResponseDto(comment, commentRecommendCount));
        }
        // 게시물좋아요레포에서 CountBy로 게시물 좋아요 가져온다.
        return new RetrieveBoardResponseDto(board, 0L, commentList);

    }

    //게시물 삭제
    @Transactional
    public void deleteBoard(Long boardId) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("찾는 게시물이 존재하지 않습니다."));
        String user = SecurityUtil.getCurrentMemberEmail();
        board.checkUser(board, user);
        boardRepository.deleteById(boardId);
    }

    //게시물 수정
    @Transactional
    public UpdateBoardResponseDto updateBoard(Long boardId, UpdateBoardRequestDto boardRequestDto) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("찾는 게시물이 존재하지 않습니다."));
        String user = SecurityUtil.getCurrentMemberEmail();
        board.checkUser(board, user);
        board.updateBoard(boardRequestDto);
        boardRepository.save(board);
        Long boardRecommendCount = boardRecommendationRepository.countByBoardId(boardId);
        return new UpdateBoardResponseDto(board, boardRecommendCount);
    }


    @Override
    @Transactional
    public String recommendBoard(Long boardId) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("찾는 게시물이 존재하지 않습니다."));
//        String user = SecurityUtil.getCurrentMemberEmail();
        Optional<BoardRecommendation> optionalBoardRecommend = boardRecommendationRepository.findByMemberAndBoardId(board.getMember(), boardId);
        if (optionalBoardRecommend.isPresent()) {
            boardRecommendationRepository.delete(optionalBoardRecommend.get());
            return "댓글 좋아요 취소완료";
        }
        BoardRecommendation boardRecommendation = new BoardRecommendation(board, board.getMember());
        boardRecommendationRepository.save(boardRecommendation);
        return "댓글 좋아요 완료";
    }

    public Pageable pageableSetting() {
        Sort.Direction direction = Sort.Direction.DESC;
        Sort sort = Sort.by(direction, "modDate");
        Pageable pageable = PageRequest.of(0, 10, sort);
        return pageable;
    }
}