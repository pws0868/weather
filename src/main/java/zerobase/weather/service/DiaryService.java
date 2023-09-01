package zerobase.weather.service;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.WeatherApplication;
import zerobase.weather.domain.DateWeather;
import zerobase.weather.domain.Diary;
import zerobase.weather.error.InvalidDate;
import zerobase.weather.repository.DateWeatherRepository;
import zerobase.weather.repository.DiaryRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class DiaryService {

    @Value("${openweathermap.key}")
    private String apiKey;

    private final DiaryRepository diaryRepository;
    private final DateWeatherRepository dateWeatherRepository;
    private static final Logger logger = LoggerFactory.getLogger(WeatherApplication.class);

    public DiaryService(DiaryRepository diaryRepository, DateWeatherRepository dateWeatherRepository) {
        this.diaryRepository = diaryRepository;
        this.dateWeatherRepository = dateWeatherRepository;
    }

    @Transactional
    @Scheduled(cron = "0 0 1 * * *") // 매일 새벽 1시마다 Scheduled 어노테이션이 작동,(cron = 초 분 시 일 월 년)
    public void saveWeatherDate() {
        logger.info("01시 날씨 데이터를 기록했습니다.");
        dateWeatherRepository.save(getWeatherFromApi());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createDiary(LocalDate date, String text) {
        logger.info("started to create diary");
        // 날씨데이터 가져오기 (API 에서 가져오기? or DB에서 가져오기?)
        DateWeather dateWeather = getDateWeather(date);

        //파싱된 데이터 + 일기 값 우리 db에 넣기
        Diary nowDiary = new Diary();
        nowDiary.setDateWeather(dateWeather); // domain/Diary setDateWeather 값이 들어온다
        nowDiary.setText(text);
        nowDiary.setDate(date);
        diaryRepository.save(nowDiary);
        logger.info("end to create diary");
    }

    private DateWeather getWeatherFromApi() { //saveWeatherDate 를 위해 만든 메서드
        // open weather map 에서 날씨데이터 가져오기
        String weatherData = getWeatherString();

        //받아온 날씨 json 파싱하기
        Map<String, Object> parseWeather = parseWeather(weatherData);
        DateWeather dateWeather = new DateWeather();
        dateWeather.setDate(LocalDate.now());
        dateWeather.setWeather(parseWeather.get("main").toString());
        dateWeather.setIcon(parseWeather.get("icon").toString());
        dateWeather.setTemperature((Double) parseWeather.get("temp"));
        return dateWeather;
    }

    private DateWeather getDateWeather(LocalDate date) {
        List<DateWeather> dateWeathersListFromDB = dateWeatherRepository.findAllByDate(date);
        if (dateWeathersListFromDB.size() == 0) {
            // 새로 api에서 날씨 정보 가져와야 함
            return getWeatherFromApi();
        }else {
            return dateWeathersListFromDB.get(0);
        }
    }
    @Transactional(readOnly = true)
    public List<Diary> readDiary(LocalDate date) {
        //너무 과거 혹은 미래의 날짜를 get 요청했을 경우 에러 반환
        if (date.isAfter(LocalDate.ofYearDay(3050, 1))) {
            throw new InvalidDate();
        }
        logger.debug("read diary");
        return diaryRepository.findAllByDate(date); // 반환값이 List<Diary> 형식
    }
    public List<Diary> readDiaries(LocalDate startDate, LocalDate endDate) { //기간내 diary를 조회하는 메서드
        return diaryRepository.findAllByDateBetween(startDate, endDate);
    }

    public void updateDiary(LocalDate date, String text) { //일기 수정을 위한 메서드
        Diary nowDiary = diaryRepository.getFirstByDate(date); //해당 날 일기에 복수의 text가 있더라도 첫번째 일기를 수정한다
        nowDiary.setText(text); //텍스트를 수정해서 set 한다
        diaryRepository.save(nowDiary); //여기서의 save 는 nowDiary id 값이 변하지 않았기 떄문에 기존 text를 덮어씌운다.
    }

    public void deleteDiary(LocalDate date) {
        diaryRepository.deleteAllByDate(date);

    }

    private String getWeatherString() {
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=seoul&appid=" + apiKey;
        try {

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(); //http 커넥션 오픈
            connection.setRequestMethod("GET"); //get 요청을 전송
            int responseCode = connection.getResponseCode(); //응답 코드를 수령
            BufferedReader br; //응답 코드를 br 객체로 BufferedReader에 내장
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            }else {
                br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = br.readLine()) != null){
                response.append(inputLine);
            }
            br.close();
            return response.toString();
            //이렇게 가져올 날씨 데이터를 위의 createDiary의 weatherData 로 넘긴다

        }catch (Exception e) {
            return "failed to get response";
        }
    }

    private Map<String, Object> parseWeather(String jsonString) {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject;

        try {
            jsonObject =(JSONObject) jsonParser.parse(jsonString);
        }catch (ParseException e) {
            throw new RuntimeException(e);
        }
        Map<String, Object> resultMap = new HashMap<>();

        JSONObject mainData =(JSONObject) jsonObject.get("main");
        resultMap.put("temp", mainData.get("temp"));

        JSONArray weatherArray =(JSONArray) jsonObject.get("weather");
        JSONObject weatherData =(JSONObject) weatherArray.get(0);
        resultMap.put("main", weatherData.get("main"));
        resultMap.put("icon", weatherData.get("icon"));
        return resultMap;
        //temp, main, icon 3가지를 hashmap 형태로 받는 메서드
    }
}
