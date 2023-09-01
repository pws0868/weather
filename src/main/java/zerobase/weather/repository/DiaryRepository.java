package zerobase.weather.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.domain.Diary;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Integer> {
    //diary service에서 사용한다
    List<Diary> findAllByDate(LocalDate date);
    //그날 전체 일기를 가져오는 함수
    List<Diary> findAllByDateBetween(LocalDate startDate, LocalDate endDate);
    //시작일과 종료일 사이의 데이터를 열람하는 함수
    Diary getFirstByDate(LocalDate date);
    //특정 날짜에 복수의 기록 text가 있어도 첫번째것을 가져오는 함수
    @Transactional
    void deleteAllByDate(LocalDate date);
    //삭ㅋ좨, 조건 거는것 가능
}
