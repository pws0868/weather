package zerobase.weather;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.domain.Memo;
import zerobase.weather.repository.JdbcMemoRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
//Transactional 이 활성화되어 있는 동안은 test에서 실행된 어떤 결과값도 commit 시키지 않는다.
public class JdbcMemoRepositoryTest {

    @Autowired
    JdbcMemoRepository jdbcMemoRepository;

    @Test
    void insertMemoTest() {

        //given --> 데이터베이스에 id 1과 해당하는 텍스트 를 삽입힌다.
        Memo newMemo = new Memo(2, "insertMemoTest");

        //when  -->  given 에서 삽입한 데이터를 레파지토리의 save 메서드를 이용해서 저장한다.
        jdbcMemoRepository.save(newMemo);

        //then  --> 저장한 데이터가 제대로 삽입되었는지 asserEquals를 이용해서 확인한다.
        Optional<Memo> result = jdbcMemoRepository.findById(2);
        assertEquals(result.get().getText(),"insertMemoTest");
    }

    @Test
    void findAllMemoTest() {
        List<Memo> memoList = jdbcMemoRepository.findAll();
        System.out.println(memoList);
        assertNotNull(memoList);
    }
}
