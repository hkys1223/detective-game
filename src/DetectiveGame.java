import javax.swing.*;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Random;
import java.util.Timer; // 메인 게임 타이머
import java.util.TimerTask;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

/**
 * 전체 게임을 실행하는 최상위 JFrame
 * 모든 미니게임 화면을 CardLayout으로 관리
 * GameManager로 게임 흐름 관리와 UI 연결, 다크 테마 적용
 */
public class DetectiveGame extends JFrame {

    // --- // UI 전체에서 공통으로 사용하는 테마 색상
    public static final Color BG_COLOR = new Color(0, 0, 0); // 검은색
    public static final Color FG_COLOR = new Color(0, 255, 100); // 밝은 녹색 (터미널)
    public static final Color BTN_BG_COLOR = new Color(30, 30, 30); // 어두운 회색
    public static final Color BTN_BORDER_COLOR = new Color(0, 200, 80); // 녹색 테두리
    
    // BGM 플레이어들
    private BgmPlayer menuBgm;  // 시작/메뉴 화면 BGM
    private BgmPlayer gameBgm;  // 메인 게임 + 미니게임 BGM

    // 화면 전환을 위한 CardLayout과 컨테이너
    private CardLayout cardLayout; //여러 패널을 이름으로 전환하기 위한 레이아웃
    private JPanel mainContainer; // CardLayout이 올라가는 최상위 컨테이너
    private GameManager gameManager; // 전체 게임 진행 로직을 관리하는 엔진

    // 실제로 화면에 보여질 패널들
    private MainGamePanel mainGamePanel; // 메인 HUD, 카드 선택 등 실제 플레이 화면
    private StartScreenPanel startScreenPanel; // 시작 화면 (타이틀, 시작 버튼 등)
    private HowToPlayPanel howToPlayPanel; // HOW TO PLAY / 도움말 화면

    // 각 미니게임 별로 따로 분리된 패널들
    private TypingMiniGamePanel typingGamePanel;
    private WireConnectingGamePanel wireGamePanel;
    private CardMatchingGamePanel matchingGamePanel;
    private SequenceMemoryGamePanel sequenceGamePanel;
    private CodeLockGamePanel codeLockGamePanel;
    private SliderPuzzleGamePanel sliderGamePanel;
    private PopupCloseGamePanel popupGamePanel;
    // 게임 엔딩(성공/실패 결과를 보여주는 화면)
    private EndGamePanel endGamePanel;

    // 메인 윈도우 생성자()
    public DetectiveGame() {
        // 툴팁 딜레이 0으로 설정 (마우스 올리자마자 툴팁 표시)
    	ToolTipManager.sharedInstance().setInitialDelay(0);
    	
        setTitle("IP.107.SECURE - 107호의 보안을 사수하라!");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // CardLayout으로 여러 화면 전환을 할 컨테이너 생성
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        mainContainer.setBackground(BG_COLOR); // 메인 컨테이너 배경색
        // 게임 매니저 생성 (UI에 대한 참조를 넘겨줌 → 나중에 화면 전환/팝업 등에 사용)
        this.gameManager = new GameManager(this);

        // Panel들 생성
        mainGamePanel = new MainGamePanel(gameManager);
        typingGamePanel = new TypingMiniGamePanel(gameManager);
        wireGamePanel = new WireConnectingGamePanel(gameManager);
        matchingGamePanel = new CardMatchingGamePanel(gameManager);
        sequenceGamePanel = new SequenceMemoryGamePanel(gameManager);
        codeLockGamePanel = new CodeLockGamePanel(gameManager);
        sliderGamePanel = new SliderPuzzleGamePanel(gameManager); // (신규 추가)
        popupGamePanel = new PopupCloseGamePanel(gameManager); // (신규 추가)
        endGamePanel = new EndGamePanel(gameManager);
        startScreenPanel = new StartScreenPanel(this);
        howToPlayPanel = new HowToPlayPanel(this);


        // Container에 Panel들 추가
        mainContainer.add(mainGamePanel, "MAIN_GAME");
        mainContainer.add(typingGamePanel, "TYPING_GAME");
        mainContainer.add(wireGamePanel, "WIRE_GAME");
        mainContainer.add(matchingGamePanel, "MATCHING_GAME");
        mainContainer.add(sequenceGamePanel, "SEQUENCE_GAME");
        mainContainer.add(codeLockGamePanel, "CODELOCK_GAME");
        mainContainer.add(sliderGamePanel, "SLIDER_GAME"); // (신규 추가)
        mainContainer.add(popupGamePanel, "POPUP_GAME"); // (신규 추가)
        mainContainer.add(endGamePanel, "END_GAME");
        mainContainer.add(startScreenPanel, "START_SCREEN");
        mainContainer.add(howToPlayPanel, "HOW_TO_PLAY");

        // 메인 컨테이너 추가
        add(mainContainer);

        // (신규) 다크 모드 UI 일괄 적용
        setDarkTheme(this);
        cardLayout.show(mainContainer, "START_SCREEN"); // 시작 화면부터 보여줌
        
        // BGM 로드
        menuBgm = new BgmPlayer("/bgm_menu.wav");  // 시작/메뉴용
        menuBgm.setVolume(-15.0f);   // 메뉴 화면 볼륨
        gameBgm = new BgmPlayer("/bgm_game.wav");  // 게임 진행용
        gameBgm.setVolume(-15.0f);   // 게임 화면 볼륨
        
        // 시작 화면이니까 메뉴 BGM부터 재생
        playBgmForScreen("START_SCREEN");
        
        // 창 닫을 때 BGM도 정리
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (menuBgm != null) menuBgm.stop();
                if (gameBgm != null) gameBgm.stop();
            }
        });
    }

    public void startNewGameFromMenu() {
        gameManager.startGame();      // 타이머 + 턴 + 카드 덱 초기화
        showScreen("MAIN_GAME");      // 실제 게임 화면으로 전환
    }
    
    // 미니게임 시작 시 상황 설명 팝업
    public void showScenarioPopup(String message) {
        GameManager.showThemedMessage(
                message,
                "상황 발생",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
    
 // 화면 이름에 따라 적절한 BGM 재생
    private void playBgmForScreen(String panelName) {
        if (menuBgm == null || gameBgm == null) return;

        boolean isMenuScreen =
                "START_SCREEN".equals(panelName) ||
                "HOW_TO_PLAY".equals(panelName);

        if (isMenuScreen) {
            // 메뉴 화면: 게임 BGM만 끄고, 메뉴 BGM은 이미 재생 중이면 유지
            gameBgm.stop();
            if (!menuBgm.isPlaying()) {
                menuBgm.playLoop();   // 처음 들어올 때만 재생
            }
        } else {
            // 게임/미니게임/엔딩: 메뉴 BGM만 끄고, 게임 BGM은 이미 재생 중이면 유지
            menuBgm.stop();
            if (!gameBgm.isPlaying()) {
                gameBgm.playLoop();
            }
        }
    }
    
    // [일괄 적용 함수] 다크 모드 적용 헬퍼
    // 컴포넌트 트리를 돌면서 Swing 기본 색을 통일된 다크 테마로 바꿔주는 메서드
    public static void setDarkTheme(Component component) {
        // [색상 규격 통일] 텍스트 입력 계열은 버튼 색에 맞춰서 별도 처리
        component.setBackground(BG_COLOR);
        component.setForeground(FG_COLOR);

        if (component instanceof JTextArea || component instanceof JTextField) {
            component.setBackground(BTN_BG_COLOR);
            component.setForeground(FG_COLOR);
            ((javax.swing.text.JTextComponent) component).setCaretColor(FG_COLOR);
            // 중요 포인트: 이미 TitledBorder가 있는 경우에는 덮어쓰지 않음
            javax.swing.border.Border border = ((JComponent) component).getBorder();
            if (!(border instanceof TitledBorder)) {
                ((JComponent) component).setBorder(
                        BorderFactory.createLineBorder(BTN_BORDER_COLOR, 1)
                );
            }
            return; // JTextArea/Field는 자식이 없음
        }
        // 버튼 공통 스타일 (배경/글자/테두리), 버튼, 텍스트 필드 등 타입별로 다른 스타일 적용
        if (component instanceof JButton) {
            JButton btn = (JButton) component;
            btn.setBackground(BTN_BG_COLOR);
            btn.setForeground(FG_COLOR);
            btn.setOpaque(true);
            btn.setContentAreaFilled(true); // 배경색 보이게
            btn.setBorder(BorderFactory.createLineBorder(BTN_BORDER_COLOR, 1));
            // 버튼 클릭 시 테두리 색상 변경 (Focus)
            btn.setFocusPainted(false);
            return; // JButton은 자식이 없음
        }
        // 스크롤 영역 배경/테두리 설정
        if (component instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) component;
            scroll.getViewport().setBackground(BG_COLOR);
            scroll.setBorder(BorderFactory.createLineBorder(BTN_BORDER_COLOR, 1));
        }
        // TitledBorder가 달린 JPanel의 제목 색상만 따로 설정
        if (component instanceof JPanel) {
            // 테두리(Border)가 TitleBorder일 경우 색상 변경
            javax.swing.border.Border border = ((JPanel) component).getBorder();
            if (border instanceof javax.swing.border.TitledBorder) {
                ((javax.swing.border.TitledBorder) border).setTitleColor(FG_COLOR);
            }
        }
        // [재귀 호출] 자식 컴포넌트가 있다면 그 안으로 들어가서 똑같이 적용
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                setDarkTheme(child); // 재귀 호출
            }
        }
    }


    // UI Control Methods (GameManager가 호출)
    public void updateMainStats(int time, int alarm, List<Clue> clues, int turn) {
        mainGamePanel.updateStats(time, alarm, clues, turn);
    }
    
    // GameManager가 부를 수 있게 래핑
    public void refreshCards() {
        if (mainGamePanel != null) {
            mainGamePanel.refreshCards();
        }
    }
    
    // cardLayout으로 현재 보여줄 화면(panel)을 변경
    public void showScreen(String panelName) { 
    	cardLayout.show(mainContainer, panelName); 
    	playBgmForScreen(panelName);
    }
    // 아래 메서드들은 각 미니게임 패널을 준비하고 해당 화면으로 전환하는 역할
    public void setupTypingGame(TypingMiniGame game) {
        // 상황 설명 → 해당 미니게임 화면으로 전환
        showScenarioPopup("로그 파일이 손상됐습니다!\n올바르게 복구하세요.");
        typingGamePanel.setGame(game); showScreen("TYPING_GAME"); }
    public void setupWireGame(WireConnectingGame game) {
    	showScenarioPopup("네트워크 케이블이 잘못 연결되었습니다.\n표시된 색 순서대로 선을 다시 연결하세요.");
        wireGamePanel.setGame(game); showScreen("WIRE_GAME"); }
    public void setupMatchingGame(CardMatchingGame game) {
    	showScenarioPopup("침입자 흔적을 추적하기 위해 로그 조각을 정리해야 합니다.\n같은 정보를 짝지어 모든 카드를 찾아내세요.");
        matchingGamePanel.setGame(game); showScreen("MATCHING_GAME"); }
    public void setupSequenceGame(SequenceMemoryGame game) {
    	showScenarioPopup("의심스러운 접속 패턴이 포착되었습니다.\n보여준 순서를 기억했다가 그대로 다시 입력하세요.");
        sequenceGamePanel.setGame(game); showScreen("SEQUENCE_GAME"); }
    public void setupCodeLockGame(CodeLockGame game) {
    	showScenarioPopup("보안 장치가 자동 잠금 상태로 전환되었습니다.\n정확한 4자리 코드를 입력해 잠금을 해제하세요.");
        codeLockGamePanel.setGame(game); showScreen("CODELOCK_GAME"); }
    public void setupSliderGame(SliderPuzzleGame game) {
    	showScenarioPopup("시스템 경로가 꼬였습니다!\n조각을 재배치하여 복구하세요.");
        sliderGamePanel.setGame(game); showScreen("SLIDER_GAME"); }
    public void setupPopupGame(PopupCloseGame game) {
    	showScenarioPopup("악성 팝업이 화면을 가리고 있습니다.팝업을 빠르게 닫으세요.");
        popupGamePanel.setGame(game); showScreen("POPUP_GAME"); }
    // 게임 종료 화면으로 전환 (성공/실패 여부와 메시지 전달)
    public void showEndScreen(boolean isWin, String message) {
        endGamePanel.setResult(isWin, message); showScreen("END_GAME"); }

    // Main Method (프로그램 실행)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DetectiveGame game = new DetectiveGame();
            game.setVisible(true);
        });
    }
}

//GameManager (게임 로직 엔진) - 랜덤 생성 로직
class GameManager {

    private DetectiveGame ui; // 화면 전환/팝업 등 UI 담당 객체
    // 메인 타이머(전체 게임 제한 시간, 초 단위), 경보 게이지, 현재 턴
    private int timeLimitSeconds, alarmGauge, currentTurn;
    private final int MAX_TURN = 25; // 최대 턴 수
    private boolean isGameRunning; // 게임 진행 중 여부
    private Timer gameTimer; // java.util.Timer로 메인 타이머 관리
    private PlayerState playerState; // 플레이어가 획득한 단서 등 상태
    private ResultChecker resultChecker; // 최종 IP 정답 체크
    private List<Card> cardDeck; // 카드 목록
    private MiniGame currentMiniGame; // 지금 플레이 중인 미니게임
    private Card currentCard; // 현재 카드 저장

    // 랜덤 생성을 위한 객체
    private Random random = new Random();

    public GameManager(DetectiveGame ui) {
        this.ui = ui;
        this.playerState = new PlayerState();
        // 카드 덱 초기화: 각 카드마다 미니게임 + 보상 단서/페널티를 설정
        this.cardDeck = new ArrayList<>();
    }
    // 게임 시작 시 초기화하는 메서드 (타이머/경보/턴/단서)
    public void startGame() {
        this.timeLimitSeconds = 300; // 5분
        this.alarmGauge = 0;
        this.currentTurn = 1;
        this.isGameRunning = true;
        this.playerState.clearClues();

        // 고정된 데이터 대신 랜덤 시나리오 생성 함수 호출!!
        generateRandomScenario();
        
        // 새 카드 덱 기준으로 버튼 다시 생성
        ui.refreshCards();
        
        // 메인 패널에 현재 상태 전달
        ui.updateMainStats(timeLimitSeconds, alarmGauge, playerState.getCollectedClues(), currentTurn);
        startMainTimer(); // 메인 타이머 시작 (1초마다 감소)
    }

    // (중요!) 랜덤 IP 및 힌트 생성 로직
    private void generateRandomScenario() {
        // [1. 역발상적 접근]
        //  p1: 1~223, p2: 10~99 (두 자리 수로 고정), p3: 0~255, p4: 1~254
        int p1 = random.nextInt(223) + 1;
        int p2 = random.nextInt(90) + 10;  // 10~99
        int p3 = random.nextInt(256);
        int p4 = random.nextInt(254) + 1;
        //랜덤 IP 설정
        String targetIp = String.format("%d.%d.%d.%d", p1, p2, p3, p4);
        this.resultChecker = new ResultChecker(targetIp);

        this.cardDeck = new ArrayList<>();

        // [2. 수식 패턴화]
        // 1번째 조각
        Clue clue1 = new Clue("1", String.format(
                "IP 1번째 조각은 로그에 그대로 남은 숫자 %d이다.", p1), Clue.Type.CORE);
        // 2번째 조각: 두 자리 수 + 십의 자리/일의 자리 분리
        int tens2 = p2 / 10; int ones2 = p2 % 10;
        Clue clue2 = new Clue("2", String.format(
                "IP 2번째 조각은 두 자리 수이고, 십의 자리는 %d, 일의 자리는 %d이다.", tens2, ones2, p2), Clue.Type.CORE);
        // 3번째 조각
        Clue clue3 = new Clue("3", String.format(
                "IP 3번째 조각은 패킷 캡처에 반복적으로 등장하는 숫자 %d이다.", p3), Clue.Type.CORE);
        // 4번째 조각: 아주 간단한 +연산만 남기기 (80 + n)
        int offset4 = p4 - 80; String clue4Text;
        if (offset4 >= 0) { clue4Text = String.format(
                "IP 4번째 조각은 HTTP 기본 포트(80)에 %d를 더한 값이다.", offset4, offset4);
        } else {
            // p4는 1~254라서 80보다 작을 수도 있음 → 그때는 빼기 표현
            clue4Text = String.format(
                "IP 4번째 조각은 HTTP 기본 포트(80)에서 %d를 뺀 값이다.",
                Math.abs(offset4), Math.abs(offset4));
        }
        Clue clue4 = new Clue("4", clue4Text, Clue.Type.CORE);

        // [3. 조건부 힌트]
        String parity = (p1 % 2 == 0) ? "짝수" : "홀수";
        Clue clue5 = new Clue("HINT1",
            "추가 분석: IP 1번째 조각은 '" + parity + "'이다.", Clue.Type.SUPPORT);

        Clue clue6 = new Clue("HINT2",
            "해킹 로그: 마지막 숫자 " + p4 + "이(가) 여러 번 반복해서 등장한다.", Clue.Type.SUPPORT);

        Clue clue7 = new Clue("HINT3",
            "메모: 네 조각을 모두 더한 값은 " + (p1 + p2 + p3 + p4) + "이다.", Clue.Type.SUPPORT);

        // [4. 가짜 단서들]
        Clue fake1 = new Clue("FAKE1",
            "손상된 파일: 'IP는 192.168.0." + random.nextInt(255)
            + "'라고 적혀 있지만, 무결성 검사에서 실패했다.", Clue.Type.FAKE);

        Clue fake2 = new Clue("FAKE2",
            "낚시성 로그: 의미 없는 포트 " + (random.nextInt(9000) + 1000)
            + " 접속 시도가 수백 번 기록되어 있다.", Clue.Type.FAKE);

        // [5. 카드 덱 구성]
        cardDeck.add(new Card(
            "PC 포렌식 (Typing)",
            2,
            new TypingMiniGame("security", 15, clue1, 10)
        ));
        cardDeck.add(new Card(
            "배전실 와이어 복구 (Wire)",
            4,
            new WireConnectingGame(25, clue2, 20)
        ));
        cardDeck.add(new Card(
            "암호 카드 해독 (Matching)",
            5,
            new CardMatchingGame(40, clue3, 25)
        ));
        cardDeck.add(new Card(
            "서버실 암호 패널 (Sequence)",
            6,
            new SequenceMemoryGame(30, clue4, 30)
        ));

        // ── 코드락 비밀번호 생성 ──
        String codePass = "";
        String codeHintText = "";
        int quizType = random.nextInt(3);

        if (quizType == 0) {
            // 패턴 1: 초기 비밀번호
            codePass = "0000";
            codeHintText = "힌트: 공장 초기화 상태 그대로의 기본 비밀번호";
        } else if (quizType == 1) {
            // 패턴 2: 단순 덧셈
            int a = 1000 + random.nextInt(4000);
            int b = 1000 + random.nextInt(3000);
            codePass = String.valueOf(a + b);
            codeHintText = "힌트: " + a + " + " + b + " = ?";
        } else {
            // 패턴 3: 키패드 2580
            codePass = "2580";
            codeHintText = "힌트: 숫자 키패드 기준, 가운데 세로 줄 (위에서 아래로)";
        }

        cardDeck.add(new Card(
            "관리자 PC 잠금 해제 (Code)",
            3,
            new CodeLockGame(codePass, 30, clue5, 15, codeHintText)
        ));
        cardDeck.add(new Card(
            "CCTV 영상 복구 (Slider)",
            7,
            new SliderPuzzleGame(60, clue6, 30)
        ));
        cardDeck.add(new Card(
            "악성 팝업 차단 (Popup)",
            5,
            new PopupCloseGame(10, 10, clue7, 20)
        ));

        // 가짜 단서 카드
        cardDeck.add(new Card(
            "위장 프록시 분석 (Typing)",
            3,
            new TypingMiniGame("decoy", 15, fake1, 5)
        ));
        cardDeck.add(new Card(
            "포트 스캐닝 (Matching)",
            4,
            new CardMatchingGame(30, fake2, 10)
        ));

        // 순서 섞기
        Collections.shuffle(cardDeck);
    }

    // 전체 게임 제한 시간을 관리하는 타이머
    private void startMainTimer() {
        if (gameTimer != null) { gameTimer.cancel(); }
        gameTimer = new Timer();
        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isGameRunning) { gameTimer.cancel(); return; }
                timeLimitSeconds--;
                // Swing UI 업데이트는 EDT에서 실행
                SwingUtilities.invokeLater(() -> {
                    ui.updateMainStats(timeLimitSeconds, alarmGauge, playerState.getCollectedClues(), currentTurn);
                });
                // 시간이 0 이하가 되면 실패 처리
                if (timeLimitSeconds <= 0) { checkGameStatus("시간 초과"); }
            }
        }, 1000, 1000);
    }
    // 각 턴에서 유저가 카드 하나를 선택했을 때 호출됨
    public void selectCard(Card card) {
        if (!isGameRunning) return;
        this.currentMiniGame = card.getMiniGame();
        this.currentCard = card;   // 어떤 카드를 플레이 중인지 기억
        // 선택된 카드에 연결된 미니게임에 따라 다른 패널로 전환
        if (currentMiniGame instanceof TypingMiniGame) {
            ui.setupTypingGame((TypingMiniGame) currentMiniGame);
        } else if (currentMiniGame instanceof WireConnectingGame) {
            ui.setupWireGame((WireConnectingGame) currentMiniGame);
        } else if (currentMiniGame instanceof CardMatchingGame) {
            ui.setupMatchingGame((CardMatchingGame) currentMiniGame);
        } else if (currentMiniGame instanceof SequenceMemoryGame) {
            ui.setupSequenceGame((SequenceMemoryGame) currentMiniGame);
        } else if (currentMiniGame instanceof CodeLockGame) {
            ui.setupCodeLockGame((CodeLockGame) currentMiniGame);
        } else if (currentMiniGame instanceof SliderPuzzleGame) {
            ui.setupSliderGame((SliderPuzzleGame) currentMiniGame);
        } else if (currentMiniGame instanceof PopupCloseGame) {
            ui.setupPopupGame((PopupCloseGame) currentMiniGame);
        }
    }
    // [확장성 구조]
    // 미니게임이 끝났을 때(성공/실패 결정 후) 호출
    public void onMiniGameComplete(boolean success) {
        if (!isGameRunning) return;
        // 메인 타이머가 이미 끝났다면 바로 게임 상태 체크
        if (timeLimitSeconds <= 0) { checkGameStatus("메인 시간 초과"); return; }

        if (success) {
            // 성공 시: 해당 미니게임 보상 단서 획득
            Clue clue = currentMiniGame.getRewardClue();
            playerState.addClue(clue);
            GameManager.showThemedMessage("성공! 단서를 획득합니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
            alarmGauge = Math.max(0, alarmGauge - 5);
            
            // 이 카드 다시 못 쓰게 표시
            if (currentCard != null) {
                currentCard.setCleared(true);
            }
        } else {
            // 실패 시: 미니게임이 가진 페널티만큼 경보 게이지 증가
            alarmGauge += currentMiniGame.getPenalty();
            GameManager.showThemedMessage("실패! 페널티가 적용됩니다.", "실패", JOptionPane.WARNING_MESSAGE);
        }
        // 턴 증가 및 상태 갱신
        currentTurn++;
        // 메인 카드 목록 다시 그리기(클리어 상태 반영)
        ui.refreshCards();
        ui.showScreen("MAIN_GAME");
        ui.updateMainStats(timeLimitSeconds, alarmGauge, playerState.getCollectedClues(), currentTurn);
        // 게임이 계속 가능한지 체크 (시간/턴/경보 상태)
        checkGameStatus("미니게임 완료");
    }
    // 최종 IP 제출 처리
    public void submitFinalAnswer(String submittedIp) {
        if (!isGameRunning) return;
        if (submittedIp == null || submittedIp.trim().isEmpty()) { return; }

        boolean isCorrect = resultChecker.checkAnswer(submittedIp.trim());

        if (isCorrect) {
            endGame(true, "승리! 범인의 IP (" + submittedIp + ")를 검거했습니다!");
        } else {
            GameManager.showThemedMessage("틀렸습니다. IP 주소가 일치하지 않습니다.", "오답", JOptionPane.ERROR_MESSAGE);
            alarmGauge += 30;
            ui.updateMainStats(timeLimitSeconds, alarmGauge, playerState.getCollectedClues(), currentTurn);
            checkGameStatus("오답 제출");
        }
    }
    // 시간/경보/턴 상태를 보고 게임 종료 조건을 체크
    private void checkGameStatus(String reason) {
        if (!isGameRunning) return;
        if (timeLimitSeconds <= 0) { endGame(false, "패배... (시간 초과)"); }
        else if (alarmGauge >= 100) { endGame(false, "패배... (경보 발동)"); }
        else if (currentTurn > MAX_TURN) { endGame(false, "패배... (제한 턴 초과)"); }
    }
    // 게임 종료 공통 처리
    private void endGame(boolean isWin, String message) {
        isGameRunning = false;
        if (gameTimer != null) { gameTimer.cancel(); }

        // 패배했을 때 정답 IP를 함께 보여주기
        if (!isWin && resultChecker != null) {
            message += "\n\n정답 IP: " + resultChecker.getCorrectIp();
        }

        final boolean finalIsWin = isWin;
        final String finalMessage = message;

        SwingUtilities.invokeLater(() -> {
            ui.showEndScreen(finalIsWin, finalMessage);
        });
    }

    // 공통 팝업: 다크 테마 적용 후 메시지 박스 띄운 다음, UIManager 원상 복구
    // 정적 메서드라서 ui 객체 없이 호출 가능
    public static void showThemedMessage(String message, String title, int type) {
        // UIManager로 JOptionPane 색상 임시 변경
        UIManager.put("Panel.background", DetectiveGame.BG_COLOR);
        UIManager.put("OptionPane.background", DetectiveGame.BG_COLOR);
        UIManager.put("OptionPane.messageForeground", DetectiveGame.FG_COLOR);
        UIManager.put("Button.background", DetectiveGame.BTN_BG_COLOR);
        UIManager.put("Button.foreground", DetectiveGame.FG_COLOR);
        UIManager.put("Button.border", BorderFactory.createLineBorder(DetectiveGame.BTN_BORDER_COLOR));

        JOptionPane.showMessageDialog(null, message, title, type);

        // 다음 팝업이나 다른 화면에 영향을 주지 않도록 기본값으로 되돌림
        UIManager.put("Panel.background", null);
        UIManager.put("OptionPane.background", null);
        UIManager.put("OptionPane.messageForeground", null);
        UIManager.put("Button.background", null);
        UIManager.put("Button.foreground", null);
        UIManager.put("Button.border", null);
    }
    // 메인 게임 화면에서 카드 목록을 그릴 때 사용
    public List<Card> getCardDeck() { return cardDeck; }
}

// UI Panels (각 화면)

/**
 * 메인 게임 화면
 * 상단: 남은 시간 / 경보 게이지 / 턴
 * 중앙: 선택 가능한 작업 카드들
 * 하단: 수집된 단서 리스트 + 최종 IP 제출 버튼
 */
class MainGamePanel extends JPanel {
    private GameManager gameManager;
    private JLabel lblTimer, lblTurn;
    private JProgressBar alarmBar; // 경보 게이지 바
    private JTextArea txtClues; // 수집된 단서 표시 영역
    private JPanel cardPanel; // 카드(선택 버튼)들이 들어가는 영역

    public MainGamePanel(GameManager gm) {
        this.gameManager = gm;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // 상단: 남은 시간, 현재 턴, 경보 게이지
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        lblTimer = new JLabel("시간: 00:00", SwingConstants.CENTER);
        lblTurn = new JLabel("턴: 1/25", SwingConstants.CENTER);
        alarmBar = new JProgressBar(0,100);
        // 텍스트 표시(예:70%)
        alarmBar.setStringPainted(true);
        alarmBar.setBackground(DetectiveGame.BG_COLOR);
        TitledBorder alarmBorder = BorderFactory.createTitledBorder("경보 게이지");
        alarmBorder.setTitleColor(DetectiveGame.FG_COLOR);
        alarmBar.setBorder(alarmBorder);
        alarmBar.setFont(new Font("Monospaced", Font.BOLD, 18));
        updateAlarmColor(0);
        
        lblTimer.setFont(new Font("Monospaced", Font.BOLD, 18));
        lblTurn.setFont(new Font("Monospaced", Font.BOLD, 18));
        statsPanel.add(lblTimer); statsPanel.add(alarmBar); statsPanel.add(lblTurn);
        add(statsPanel, BorderLayout.NORTH);


        // 중앙: 카드(작업 선택 버튼) 영역
        // FlowLayout -> GridLayout(0, 4)로 4개씩 자동 줄바꿈
        cardPanel = new JPanel(new GridLayout(0, 4, 20, 20));
        cardPanel.setBorder(BorderFactory.createTitledBorder("작업 선택"));
        // GridLayout 패널에도 배경색 직접 적용
        cardPanel.setBackground(DetectiveGame.BG_COLOR);

        add(new JScrollPane(cardPanel), BorderLayout.CENTER);
        setupCardButtons();
        // 하단: 단서 창 + 최종 제출 버튼
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
        txtClues = new JTextArea(8, 30);
        txtClues.setEditable(false);
        TitledBorder clueBorder = BorderFactory.createTitledBorder("수집된 단서 (범인의 IP 조각)");
        clueBorder.setTitleColor(DetectiveGame.FG_COLOR); // 다크테마용 제목 색
        txtClues.setBorder(clueBorder);
        txtClues.setFont(new Font("Monospaced", Font.PLAIN, 14));
        bottomPanel.add(new JScrollPane(txtClues), BorderLayout.CENTER);
        // 최종 IP 제출 버튼
        JButton btnSubmit = new JButton("최종 결론 도출 (범인 IP 제출)");
        btnSubmit.setFont(new Font("SansSerif", Font.BOLD, 16));

        // 다크 테마 적용
        DetectiveGame.setDarkTheme(btnSubmit);
        // 마우스를 올리면 버튼이 약간 밝은 회색으로 변함
        Color submitNormalBg = btnSubmit.getBackground();
        Color submitHoverBg  = new Color(60, 60, 60); // 밝은 회색
        // 마우스 올렸을 때 / 나갔을 때 색 변경
        btnSubmit.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnSubmit.setBackground(submitHoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnSubmit.setBackground(submitNormalBg);
            }
        });
        // 최종 IP 입력 다이얼로그 띄우고 GameManager에 전달
        btnSubmit.addActionListener(e -> {
            // 최종 IP 입력을 받는 다크 테마 기반 입력 다이얼로그
            UIManager.put("Panel.background", DetectiveGame.BG_COLOR);
            UIManager.put("OptionPane.background", DetectiveGame.BG_COLOR);
            UIManager.put("OptionPane.messageForeground", DetectiveGame.FG_COLOR);
            UIManager.put("Button.background", DetectiveGame.BTN_BG_COLOR);
            UIManager.put("Button.foreground", DetectiveGame.FG_COLOR);
            UIManager.put("Button.border", BorderFactory.createLineBorder(DetectiveGame.BTN_BORDER_COLOR));
            UIManager.put("TextField.background", DetectiveGame.BTN_BG_COLOR);
            UIManager.put("TextField.foreground", DetectiveGame.FG_COLOR);
            UIManager.put("TextField.caretForeground", DetectiveGame.FG_COLOR);

            String ip = JOptionPane.showInputDialog(this,
                    "범인의 IP 주소를 입력하세요 (예: 107.192.1.88)",
                    "최종 결론",
                    JOptionPane.QUESTION_MESSAGE);

            // UIManager 원상 복구 (다른 팝업에 영향 방지를 위해서)
            UIManager.put("Panel.background", null); UIManager.put("OptionPane.background", null);
            UIManager.put("OptionPane.messageForeground", null); UIManager.put("Button.background", null);
            UIManager.put("Button.foreground", null); UIManager.put("Button.border", null);
            UIManager.put("TextField.background", null); UIManager.put("TextField.foreground", null);
            UIManager.put("TextField.caretForeground", null);

            gameManager.submitFinalAnswer(ip);
        });
        bottomPanel.add(btnSubmit, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        DetectiveGame.setDarkTheme(this); // 여기 패널 전체에 테마 적용
    }
    
    // 외부에서 다시 카드 버튼들을 새로 만들 때 사용
    public void refreshCards() {
        setupCardButtons();
    }
    
    // 카드 버튼을 다시 그릴 때 호출 (턴 시작 시 등)
    private void setupCardButtons() {
        cardPanel.removeAll();
        List<Card> deck = gameManager.getCardDeck();
        for (Card card : deck) {
        	MiniGame mini = card.getMiniGame();
            JButton btnCard = new JButton(String.format("<html><center>%s<br>(난이도: %d)</center></html>",
                    card.getTitle(), card.getDifficulty()));
            // 공통 테마 먼저 적용
            DetectiveGame.setDarkTheme(btnCard);
            
            // GridLayout이 버튼 크기를 관리
            // 툴팁: 미니게임 정보 + 보상/페널티/시간 안내
            // 툴팁 설정 (HTML 써서 줄바꿈)
            String tooltip = String.format(
                    "<html>"
                    + "<b>%s</b><br>"
                    + "성공 시: 단서 획득<br>"
                    + "실패 시: 경보 +%d%%<br>"
                    + "제한 시간: %d초"
                    + "</html>",
                    mini.getGameName(), // 미니게임 이름
                    mini.getPenalty(), // 경보 페널티
                    mini.getTimeLimitSeconds() // 미니게임 제한시간
                );
                btnCard.setToolTipText(tooltip);
            
                // 이미 클리어된 카드라면 흐리게 + 비활성화
                if (card.isCleared()) {
                    btnCard.setEnabled(false);
                    btnCard.setForeground(new Color(120, 120, 120));

                    btnCard.setText(String.format(
                            "<html>"
                            + "<center>%s<br>"
                            + "<span style='font-size:10px;"
                            + "color:gray;'>(클리어)</span></center>"
                            + "</html>",
                            card.getTitle()));
                } else {
                    // 아직 클리어하지 않은 카드만 클릭 가능
                    btnCard.addActionListener(e ->
                            gameManager.selectCard(card));

                    // Hover 효과
                    Color normalBg = btnCard.getBackground();
                    Color hoverBg  = new Color(60, 60, 60);

                    btnCard.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseEntered(MouseEvent e) {
                            btnCard.setBackground(hoverBg);
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                            btnCard.setBackground(normalBg);
                        }
                    });
                }
                cardPanel.add(btnCard);
            }
            cardPanel.revalidate();
            cardPanel.repaint();
        }
    
    // GameManager에서 호출하는 상태 갱신 메서드
    public void updateStats(int time, int alarm, List<Clue> clues, int turn) {
        // 1. [방어적 프로그래밍]
        // 화면엔 버튼이 없는데, 데이터엔 카드가 있다면 -> 버튼을 생성
        if (cardPanel.getComponentCount() == 0 && !gameManager.getCardDeck().isEmpty()) {
            setupCardButtons();
        }

        int minutes = time / 60; int seconds = time % 60;
        lblTimer.setText(String.format("남은 시간: %02d:%02d", minutes, seconds));
        // 경보 게이지 값/텍스트/컬러 갱신
        alarmBar.setValue(alarm);
        alarmBar.setString(alarm + "%");
        updateAlarmColor(alarm); // 경보 수치에 따라 그라데이션 색 적용

        lblTurn.setText(String.format("턴: %d / %d", turn, 25));

        // 수집된 모든 단서 내용을 텍스트 영ㅇ역에 그대로 표시
        StringBuilder clueText = new StringBuilder();
        for (Clue clue : clues) {

            String prefix;
            switch (clue.getType()) {
                case CORE:
                    prefix = "[핵심 단서] ";
                    break;
                case SUPPORT:
                    prefix = "[추가 분석] ";
                    break;
                case FAKE:
                default:
                    prefix = "[의심 로그] ";
                    break;
            }

            clueText.append(prefix)
                    .append(clue.getContent())
                    .append("\n");
        }
        txtClues.setText(clueText.toString());
    }
    
 // 경보 값에 따라 색을 초록 → 노랑 → 빨강으로 그라데이션
    private void updateAlarmColor(int alarm) {
        // 값이 0~100을 벗어나도 오류 발생하지 않게 설정
        alarm = Math.max(0, Math.min(100, alarm));

        int r, g;

        if (alarm <= 50) {
            // 0 ~ 50 : 초록(0,255,0) → 노랑(255,255,0)
            float t = alarm / 50.0f;  // 0.0 ~ 1.0
            r = (int)(255 * t);       // 0 → 255
            g = 255;                  // 항상 255
        } else {
            // 50 ~ 100 : 노랑(255,255,0) → 빨강(255,0,0)
            float t = (alarm - 50) / 50.0f; // 0.0 ~ 1.0
            r = 255;                        // 항상 255
            g = (int)(255 * (1.0f - t));    // 255 → 0
        }

        Color gradColor = new Color(r, g, 0);
        alarmBar.setForeground(gradColor);
    }
}

class BgmPlayer {
    private Clip clip;

    public BgmPlayer(String resourcePath) {
        try {
            URL url = getClass().getResource(resourcePath);
            if (url == null) {
                throw new IllegalArgumentException("BGM 파일을 찾을 수 없음: " + resourcePath);
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            clip = AudioSystem.getClip();
            clip.open(ais);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void playLoop() {
        if (clip == null) return;
        if (clip.isRunning()) clip.stop();
        clip.setFramePosition(0);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        clip.start();
    }

    public void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }
    
    public void setVolume(float value) {
        if (clip == null) return;
        // value: -80.0f(거의 무음) ~ 6.0f(최대)
        FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        gainControl.setValue(value);
    }
    
    public boolean isPlaying() {
        return clip != null && clip.isRunning();
    }
}

/**
 * 1: 타이핑 게임
 * 주어진 문자열을 제한 시간 안에 정확히 입력해야 성공
 */
class TypingMiniGamePanel extends JPanel {
    private GameManager gameManager;
    private TypingMiniGame currentGame;
    private JLabel lblTargetText, lblTimer;
    private JTextField txtInput;
    private javax.swing.Timer gameTimer;
    private int timeLeft;

    public TypingMiniGamePanel(GameManager gm) {
        this.gameManager = gm;

        // 🔹 기존: setLayout(new GridBagLayout());
        // ⬇ 이렇게 변경
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("미니게임: 타이핑"));

        // ==== 1. 상단 오른쪽에만 나가기 버튼 ====
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btnExit = new JButton("나가기");
        btnExit.setFont(new Font("SansSerif", Font.PLAIN, 14));
        topBar.add(btnExit);
        DetectiveGame.setDarkTheme(topBar);
        DetectiveGame.setDarkTheme(btnExit);
        add(topBar, BorderLayout.NORTH);

        // ==== 2. 기존 내용은 centerPanel로 그대로 이동 ====
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        lblTimer = new JLabel("남은 시간: 00초");
        lblTimer.setFont(new Font("Monospaced", Font.BOLD, 16));
        lblTimer.setForeground(Color.RED);
        gbc.gridy = 0;
        centerPanel.add(lblTimer, gbc);

        gbc.gridy = 1;
        centerPanel.add(new JLabel("표시되는 암호를 정확히 입력하세요!"), gbc);

        lblTargetText = new JLabel("target_text");
        lblTargetText.setFont(new Font("Monospaced", Font.BOLD, 24));
        lblTargetText.setForeground(Color.CYAN);
        gbc.gridy = 2;
        centerPanel.add(lblTargetText, gbc);

        txtInput = new JTextField(20);
        txtInput.setFont(new Font("Monospaced", Font.PLAIN, 20));
        gbc.gridy = 3;
        centerPanel.add(txtInput, gbc);

        JButton btnSubmit = new JButton("입력 완료");
        btnSubmit.setFont(new Font("SansSerif", Font.BOLD, 16));
        gbc.gridy = 4;
        centerPanel.add(btnSubmit, gbc);

        DetectiveGame.setDarkTheme(centerPanel);
        DetectiveGame.setDarkTheme(btnSubmit);

        add(centerPanel, BorderLayout.CENTER);

        // ==== 3. 로직은 기존 그대로 ====
        ActionListener submitAction = e -> {
            gameTimer.stop();
            boolean success = currentGame.checkAnswer(txtInput.getText());
            txtInput.setText("");
            gameManager.onMiniGameComplete(success);
        };
        txtInput.addActionListener(submitAction);
        btnSubmit.addActionListener(submitAction);

        // 나가기: 그냥 실패 처리
        btnExit.addActionListener(e -> {
            if (gameTimer.isRunning()) gameTimer.stop();
            txtInput.setText("");
            gameManager.onMiniGameComplete(false);
        });

        gameTimer = new javax.swing.Timer(1000, e -> {
            timeLeft--;
            lblTimer.setText(String.format("남은 시간: %02d초", timeLeft));
            if (timeLeft <= 0) {
                gameTimer.stop();
                GameManager.showThemedMessage("시간 초과!", "실패", JOptionPane.WARNING_MESSAGE);
                txtInput.setText("");
                gameManager.onMiniGameComplete(false);
            }
        });

        DetectiveGame.setDarkTheme(this);
    }

    public void setGame(TypingMiniGame game) {
        this.currentGame = game;
        lblTargetText.setText(game.getTargetText());
        this.timeLeft = game.getTimeLimitSeconds();
        lblTimer.setText(String.format("남은 시간: %02d초", timeLeft));
        gameTimer.start();
        txtInput.requestFocusInWindow();
    }
}


/**
 * 2: 와이어 연결
 * 양쪽에 있는 동일 색상의 노드를 1:1로 연결해야 하는 미니게임
 */
class WireConnectingGamePanel extends JPanel {
    private GameManager gameManager;
    private WireConnectingGame currentGame;
    private static final int NODE_SIZE = 40;
    // 왼쪽/오른쪽 색상 노드 위치
    private Map<Color, Point> leftNodes = new HashMap<>();
    private Map<Color, Point> rightNodes = new HashMap<>();
    // 이미 연결 완료된 색상 목록
    private Set<Color> completedConnections = new HashSet<>();
    // 현재 선택된 색상 및 시작점
    private Color selectedColor = null;
    private Point mousePos = null;
    private Point selectedPoint = null;

    private JLabel lblTimer;
    private javax.swing.Timer gameTimer;
    private int timeLeft;

    public WireConnectingGamePanel(GameManager gm) {
        this.gameManager = gm;
        setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createTitledBorder("미니게임: 와이어 연결"));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // 왼쪽: 기존 텍스트 + 타이머
        JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        leftBox.add(new JLabel("같은 색의 노드를 연결하세요. (총 4쌍)"));
        lblTimer = new JLabel("남은 시간: 00초");
        lblTimer.setFont(new Font("Monospaced", Font.BOLD, 16));
        lblTimer.setForeground(Color.RED);
        leftBox.add(lblTimer);

        // 오른쪽: 나가기 버튼
        JButton btnExit = new JButton("나가기");
        btnExit.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightBox.add(btnExit);

        topPanel.add(leftBox, BorderLayout.WEST);
        topPanel.add(rightBox, BorderLayout.EAST);

        DetectiveGame.setDarkTheme(leftBox);
        DetectiveGame.setDarkTheme(rightBox);
        DetectiveGame.setDarkTheme(btnExit);

        this.add(topPanel, BorderLayout.NORTH);

        // 나가기 버튼 동작
        btnExit.addActionListener(e -> {
            if (gameTimer.isRunning()) gameTimer.stop();
            gameManager.onMiniGameComplete(false);
        });
        
        // 1초마다 남은 시간 감소
        gameTimer = new javax.swing.Timer(1000, e -> {
            timeLeft--;
            lblTimer.setText(String.format("남은 시간: %02d초", timeLeft));
            if (timeLeft <= 0) {
                gameTimer.stop();
                GameManager.showThemedMessage("시간 초과!", "실패", JOptionPane.WARNING_MESSAGE);
                gameManager.onMiniGameComplete(false);
            }
        });
        // 마우스 클릭으로 노드 선택/연결 처리
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!gameTimer.isRunning()) return;

                // 이미 색을 선택한 상태라면 → 두 번째 클릭을 먼저 처리해야 함
                if (selectedColor != null) {

                    boolean matched = false;
                    // 오른쪽 노드 중 같은 색상 찾기
                    for (Map.Entry<Color, Point> entry : rightNodes.entrySet()) {
                        if (isClickInside(e.getPoint(), entry.getValue())) {
                            if (entry.getKey().equals(selectedColor)) matched = true;
                        }
                    }
                    // 왼쪽 노드 중 같은 색상 찾기
                    for (Map.Entry<Color, Point> entry : leftNodes.entrySet()) {
                        if (isClickInside(e.getPoint(), entry.getValue())) {
                            if (entry.getKey().equals(selectedColor)) matched = true;
                        }
                    }

                    if (matched) {
                        // 올바른 연결
                        completedConnections.add(selectedColor);
                        selectedColor = null;      // 연결 끝
                        selectedPoint = null;
                        repaint();
                        // 모든 색상이 연결되면 성공
                        if (completedConnections.size() == currentGame.getColors().size()) {
                            gameTimer.stop();
                            gameManager.onMiniGameComplete(true);
                        }
                    } else {
                        // 실패
                        GameManager.showThemedMessage("잘못된 연결입니다!", "실패", JOptionPane.ERROR_MESSAGE);
                        selectedColor = null;
                        selectedPoint = null;
                        gameTimer.stop();
                        gameManager.onMiniGameComplete(false);
                    }

                    return;
                }

                // 첫 번째 클릭 처리: 아직 색상이 선택되지 않은 상태
                // 왼쪽 노드 클릭 체크
                for (Map.Entry<Color, Point> entry : leftNodes.entrySet()) {
                    if (isClickInside(e.getPoint(), entry.getValue())
                            && !completedConnections.contains(entry.getKey())) {

                        selectedColor = entry.getKey();
                        selectedPoint = entry.getValue();   // 시작 좌표
                        return;
                    }
                }

                // 오른쪽 노드 클릭 체크
                for (Map.Entry<Color, Point> entry : rightNodes.entrySet()) {
                    if (isClickInside(e.getPoint(), entry.getValue())
                            && !completedConnections.contains(entry.getKey())) {

                        selectedColor = entry.getKey();
                        selectedPoint = entry.getValue();   // 시작 좌표
                        return;
                    }
                }
            }
        });
        // 마우스 움직일 때 선이 따라오게 만듬
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                if (selectedColor != null) { mousePos = e.getPoint(); repaint(); }
            }
        });
        DetectiveGame.setDarkTheme(this);
    }
    // GameManager가 미니게임 시작 시 호출
    public void setGame(WireConnectingGame game) {
        this.currentGame = game;
        this.completedConnections.clear();
        this.selectedColor = null;
        this.selectedPoint = null;
        // 실제 노드의 배열은 랜덤 셔플로 함
        List<Color> colors = new ArrayList<>(game.getColors());
        List<Color> shuffledColors = new ArrayList<>(colors);
        Collections.shuffle(shuffledColors);
        leftNodes.clear(); rightNodes.clear();
        int y_start = 100, y_gap = 80, x_left = 50, x_right = 700;
        for (int i = 0; i < colors.size(); i++) {
            leftNodes.put(colors.get(i), new Point(x_left, y_start + i * y_gap));
            rightNodes.put(shuffledColors.get(i), new Point(x_right, y_start + i * y_gap));
        }
        this.timeLeft = game.getTimeLimitSeconds();
        lblTimer.setText(String.format("남은 시간: %02d초", timeLeft));
        gameTimer.start();
        repaint();
    }
    // 클릭 지점이 노드 사각형 안인지 체크
    private boolean isClickInside(Point click, Point nodePos) {
        return (click.x >= nodePos.x && click.x <= nodePos.x + NODE_SIZE &&
                click.y >= nodePos.y && click.y <= nodePos.y + NODE_SIZE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // 다크모드 배경색을 위해 super 호출
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(5));
        if (currentGame == null) return;
        int x_left = 50, x_right = getWidth() - 100, y_start = 100, y_gap = 80;
        List<Color> colors = currentGame.getColors();
        List<Color> rightOrder = new ArrayList<>(rightNodes.keySet());
        // 왼쪽/오른쪽 노드 사각형 그리기
        for (int i = 0; i < colors.size(); i++) {
            Color leftColor = colors.get(i);
            Point leftPos = new Point(x_left, y_start + i * y_gap);
            leftNodes.put(leftColor, leftPos);
            g.setColor(leftColor); g.fillRect(leftPos.x, leftPos.y, NODE_SIZE, NODE_SIZE);

            Color rightColor = rightOrder.get(i);
            Point rightPos = new Point(x_right, y_start + i * y_gap);
            rightNodes.put(rightColor, rightPos);
            g.setColor(rightColor); g.fillRect(rightPos.x, rightPos.y, NODE_SIZE, NODE_SIZE);
        }
        // 이미 연결을 완료한 색상에 대해 선 그리기
        for (Color color : completedConnections) {
            Point p1 = getCenter(leftNodes.get(color));
            Point p2 = getCenter(rightNodes.get(color));
            g.setColor(color); g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
        // 현재 선택된 색상이 있을 경우에 마우스 위치까지 임시 선 표시
        if (selectedColor != null && mousePos != null) {
            Point p1 = getCenter(selectedPoint);
            g.setColor(selectedColor); g.drawLine(p1.x, p1.y, mousePos.x, mousePos.y);
        }
    }
    // 사각형 중심 좌표 계산함
    private Point getCenter(Point nodePos) {
        return new Point(nodePos.x + NODE_SIZE / 2, nodePos.y + NODE_SIZE / 2);
    }
}


/**
 * 3: 카드 짝 맞추기
 * 12장의 카드(6쌍)에서 같은 기호를 찾는 미니게임
 */
class CardMatchingGamePanel extends JPanel {
    private GameManager gameManager;
    private CardMatchingGame currentGame;
    private List<JButton> cardButtons = new ArrayList<>();
    private List<String> symbols;
    private int pairsFound;
    private JButton firstCard = null, secondCard = null;
    private javax.swing.Timer mismatchTimer;
    private JLabel lblTimer;
    private javax.swing.Timer gameTimer;
    private int timeLeft;

    public CardMatchingGamePanel(GameManager gm) {
        this.gameManager = gm;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder("미니게임: 카드 짝 맞추기"));

        // 상단 패널: 왼쪽에 설명+타이머, 오른쪽에 나가기 버튼
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // 왼쪽: 설명 + 타이머
        JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        leftBox.add(new JLabel("같은 기호의 카드 2장을 찾으세요. (총 6쌍)"));

        lblTimer = new JLabel("남은 시간: 00초");
        lblTimer.setFont(new Font("Monospaced", Font.BOLD, 16));
        lblTimer.setForeground(Color.RED);
        leftBox.add(lblTimer);

        // 오른쪽: 나가기 버튼
        JButton btnExit = new JButton("나가기");
        btnExit.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightBox.add(btnExit);

        topPanel.add(leftBox, BorderLayout.WEST);
        topPanel.add(rightBox, BorderLayout.EAST);

        DetectiveGame.setDarkTheme(leftBox);
        DetectiveGame.setDarkTheme(rightBox);
        DetectiveGame.setDarkTheme(btnExit);

        add(topPanel, BorderLayout.NORTH);

        // 나가기 버튼 동작
        btnExit.addActionListener(e -> {
            if (gameTimer.isRunning()) gameTimer.stop();
            mismatchTimer.stop();      // 뒤집기 타이머도 멈춰주기
            gameManager.onMiniGameComplete(false);  // 실패 처리
        });

        JPanel gamePanel = new JPanel(new GridLayout(3, 4, 10, 10));
        gamePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        // 12개의 카드 버튼 생성
        for (int i = 0; i < 12; i++) {
            JButton card = new JButton("?");
            card.setFont(new Font("Monospaced", Font.BOLD, 24));
            card.setPreferredSize(new Dimension(100, 100));
            card.addActionListener(new CardClickListener());
            cardButtons.add(card);
            gamePanel.add(card);
        }
        add(gamePanel, BorderLayout.CENTER);

        // 카드가 맞지 않았을 때 다시 뒤집는 타이머
        mismatchTimer = new javax.swing.Timer(800, e -> {
            firstCard.setText("?"); secondCard.setText("?");
            firstCard.setEnabled(true); secondCard.setEnabled(true);
            firstCard = null; secondCard = null;
        });
        mismatchTimer.setRepeats(false);
        // 남은 시간 타이머
        gameTimer = new javax.swing.Timer(1000, e -> {
            timeLeft--;
            lblTimer.setText(String.format("남은 시간: %02d초", timeLeft));
            if (timeLeft <= 0) {
                gameTimer.stop(); mismatchTimer.stop();
                GameManager.showThemedMessage("시간 초과!", "실패", JOptionPane.WARNING_MESSAGE);
                gameManager.onMiniGameComplete(false);
            }
        });
        DetectiveGame.setDarkTheme(this);
    }
    // GameManager가 미니게임 시작 시 호출
    public void setGame(CardMatchingGame game) {
        this.currentGame = game;
        this.symbols = game.getShuffledSymbols();
        this.pairsFound = 0; this.firstCard = null; this.secondCard = null;
        // 각 카드 초기화
        for (int i = 0; i < cardButtons.size(); i++) {
            JButton card = cardButtons.get(i);
            card.setText("?"); card.setEnabled(true);
            card.setBackground(DetectiveGame.BTN_BG_COLOR); // 테마 적용
            card.setForeground(DetectiveGame.FG_COLOR); // 테마 적용
            card.setName(Integer.toString(i)); // 버튼 인덱스를 name에 저장
            card.setVisible(true);
        }
        this.timeLeft = game.getTimeLimitSeconds();
        lblTimer.setText(String.format("남은 시간: %02d초", timeLeft));
        gameTimer.start();
    }
    // 카드 클릭 로직
    private class CardClickListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // 두 번째 카드가 이미 선택되어 있거나, 타이머가 멈춘 상태면 무시
            if (secondCard != null || !gameTimer.isRunning()) { return; }
            JButton clickedCard = (JButton) e.getSource();
            int index = Integer.parseInt(clickedCard.getName());
            String symbol = symbols.get(index);
            // 카드 열기
            clickedCard.setText(symbol);
            clickedCard.setEnabled(false);
            // 맞추려고 선택했을 때 임시 하이라이트
            clickedCard.setBackground(new Color(0, 255, 100));

            if (firstCard == null) { firstCard = clickedCard; }
            else {
                secondCard = clickedCard;
                // 두 카드가 같은 기호인지 체크
                if (firstCard.getText().equals(secondCard.getText())) {
                    pairsFound++;
                    // 잠깐 보이게 했다가 아예 사라지게 처리
                    javax.swing.Timer matchedTimer = new javax.swing.Timer(500, ev -> {

                        // 매칭된 카드 사라지게 함
                        firstCard.setVisible(false);
                        secondCard.setVisible(false);
                        firstCard.setEnabled(false);
                        secondCard.setEnabled(false);

                        firstCard = null;
                        secondCard = null;

                        // 모든 쌍을 찾았으면 성공 처리
                        if (pairsFound == currentGame.getTotalPairs()) {
                            gameTimer.stop();
                            gameManager.onMiniGameComplete(true);
                        }
                    });

                    matchedTimer.setRepeats(false);
                    matchedTimer.start();   // 실행

                } else { // 틀렸을 때 잠깐 보여줬다가 다시 뒤집기, 다시 원래 색으로 돌아오게 설정
                    mismatchTimer = new javax.swing.Timer(700, ev -> {
                        firstCard.setText("?");
                        secondCard.setText("?");

                        firstCard.setEnabled(true);
                        secondCard.setEnabled(true);

                        // 네온 강조 → 다시 기본 버튼 색으로 복구
                        firstCard.setBackground(DetectiveGame.BTN_BG_COLOR);
                        secondCard.setBackground(DetectiveGame.BTN_BG_COLOR);

                        firstCard = null;
                        secondCard = null;
                    });
                    mismatchTimer.setRepeats(false);
                    mismatchTimer.start();   }
            }
        }
    }
}

/**
 * 4: 순서 기억
 * 4개의 버튼이 특정 순서로 깜빡이고, 플레이어가 같은 순서대로 눌러야 성공
 */
class SequenceMemoryGamePanel extends JPanel {
    private GameManager gameManager;
    private SequenceMemoryGame currentGame;
    private List<Integer> sequence;
    private int currentStep = 0;
    private boolean playerTurn = false;
    private JButton[] buttons;
    private JTextArea display;
    private javax.swing.Timer flashTimer;
    private JLabel lblTimer;
    private javax.swing.Timer gameTimer;
    private int timeLeft;
    private Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW}; // (신규) 원본 색상 저장

    public SequenceMemoryGamePanel(GameManager gm) {
        this.gameManager = gm;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder("미니게임: 순서 기억"));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // 왼쪽: 안내 텍스트 + 타이머
        JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        display = new JTextArea(2, 30);
        display.setEditable(false);
        display.setFont(new Font("Monospaced", Font.BOLD, 14));
        leftBox.add(display);

        lblTimer = new JLabel("남은 시간: 00초");
        lblTimer.setFont(new Font("Monospaced", Font.BOLD, 16));
        lblTimer.setForeground(Color.RED);
        leftBox.add(lblTimer);

        // 오른쪽: 나가기 버튼
        JButton btnExit = new JButton("나가기");
        btnExit.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightBox.add(btnExit);

        topPanel.add(leftBox, BorderLayout.WEST);
        topPanel.add(rightBox, BorderLayout.EAST);

        DetectiveGame.setDarkTheme(leftBox);
        DetectiveGame.setDarkTheme(rightBox);
        DetectiveGame.setDarkTheme(btnExit);

        add(topPanel, BorderLayout.NORTH);

        // 나가기 버튼 동작
        btnExit.addActionListener(e -> {
            if (gameTimer.isRunning()) gameTimer.stop();
            if (flashTimer != null && flashTimer.isRunning()) {
                flashTimer.stop();
            }
            playerTurn = false;
            gameManager.onMiniGameComplete(false);
        });

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 100, 20, 100));
        buttons = new JButton[4];
        // 4개의 색깔 버튼 생성
        for (int i = 0; i < 4; i++) {
            buttons[i] = new JButton();
            buttons[i].setBackground(DetectiveGame.BTN_BG_COLOR);  // 기본색: 테마용 어두운색
            buttons[i].setOpaque(true);
            buttons[i].setBorderPainted(false); // 색상이 잘 보이도록 설정함
            buttons[i].setPreferredSize(new Dimension(100, 100));
            final int index = i;
            buttons[i].addActionListener(e -> onPlayerInput(index));
            buttonPanel.add(buttons[i]);
        }
        add(buttonPanel, BorderLayout.CENTER);
        // 남은 시간 타이머
        gameTimer = new javax.swing.Timer(1000, e -> {
            timeLeft--;
            lblTimer.setText(String.format("남은 시간: %02d초", timeLeft));
            if (timeLeft <= 0) {
                gameTimer.stop();
                if (flashTimer != null) flashTimer.stop();
                GameManager.showThemedMessage("시간 초과!", "실패", JOptionPane.WARNING_MESSAGE);
                gameManager.onMiniGameComplete(false);
            }
        });
        flashTimer = new javax.swing.Timer(600, null); // 깜빡이는 타이머(초기화)
        DetectiveGame.setDarkTheme(this);
    }
    // GameManager가 미니게임 시작 시 호출
    public void setGame(SequenceMemoryGame game) {
        this.currentGame = game;

        // 이전 flashTimer 초기화
        if (flashTimer != null) {
            flashTimer.stop();   // 여기까지만
        }

        // 버튼 상태 초기화
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setBackground(DetectiveGame.BTN_BG_COLOR); // 다크 테마 기반 기본색
            buttons[i].setEnabled(true);
            buttons[i].setBorderPainted(false);
            buttons[i].setOpaque(true);
        }

        // 게임 로직 초기화
        this.sequence = game.getSequence();
        this.currentStep = 0;
        this.playerTurn = false;

        // 타이머 초기화
        this.timeLeft = game.getTimeLimitSeconds();
        lblTimer.setText(String.format("남은 시간: %02d초", timeLeft));
        gameTimer.start();

        // 표시(애니메이션) 시작
        startSequenceDisplay();
    }
    // 버튼 깜빡임을 화면에 한 번 보여주는 애니메이션
    private void startSequenceDisplay() {
        playerTurn = false;
        display.setText("순서를 기억하세요...");
        currentStep = 0;

        ActionListener task = new ActionListener() {
            int flashCount = 0; boolean isLit = false;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (flashCount >= sequence.size()) {
                    // 모든 시퀀스를 다 보여줬다면, 플레이어 차례로 전환
                    ((javax.swing.Timer) e.getSource()).stop();
                    playerTurn = true;
                    display.setText("순서대로 누르세요!");
                    currentStep = 0; return;
                }
                int buttonIndex = sequence.get(flashCount);
                if (!isLit) {
                    // 밝게 켜기
                    buttons[buttonIndex].setBackground(colors[buttonIndex].brighter().brighter()); isLit = true;
                } else {
                    // 다시 원래 색으로 꺼준다
                    buttons[buttonIndex].setBackground(colors[buttonIndex]); isLit = false; flashCount++;
                }
            }
        };
        flashTimer.stop();
        flashTimer = new javax.swing.Timer(600, task);
        flashTimer.setInitialDelay(1000);
        flashTimer.start();
    }
    // 플레이어가 버튼을 누를 때 호출
    private void onPlayerInput(int index) {
        if (!playerTurn || !gameTimer.isRunning()) return;
        // 정답 시퀀스의 현재 단계와 비교
        if (sequence.get(currentStep) == index) {
            currentStep++;
            // 시퀀스를 끝까지 맞췄으면 성공
            if (currentStep == sequence.size()) {
                gameTimer.stop(); flashTimer.stop(); playerTurn = false;
                gameManager.onMiniGameComplete(true);
            }
        } else {
            // 틀리면 즉시 실패 처리
            gameTimer.stop(); flashTimer.stop(); playerTurn = false;
            gameManager.onMiniGameComplete(false);
        }
    }
}

/**
 * 5: 4자리 코드락
 * 4자리 숫자 입력, 3회(MAX_ATTEMPTS) 초과 시 자동 실패
 */
class CodeLockGamePanel extends JPanel {
    private GameManager gameManager;
    private CodeLockGame currentGame;
    private JTextField display; // 현재 입력된 코드 표시
    private String currentInput = "";
    private JLabel lblTimer;
    private javax.swing.Timer gameTimer;
    private int timeLeft;
    private JLabel lblHint; // 힌트 레이블
    private int wrongAttempts = 0; // 현재까지 틀린 횟수
    private final int MAX_ATTEMPTS = 3; // 최대 허용 오답 수

    public CodeLockGamePanel(GameManager gm) {
        this.gameManager = gm;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder("미니게임: 4자리 코드락"));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // 왼쪽: 안내 + 타이머
        JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        leftBox.add(new JLabel("4자리 비밀번호를 입력하세요."));

        lblTimer = new JLabel("남은 시간: 00초");
        lblTimer.setFont(new Font("Monospaced", Font.BOLD, 16));
        lblTimer.setForeground(Color.RED);
        leftBox.add(lblTimer);

        // 오른쪽: 나가기 버튼
        JButton btnExit = new JButton("나가기");
        btnExit.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightBox.add(btnExit);

        topPanel.add(leftBox, BorderLayout.WEST);
        topPanel.add(rightBox, BorderLayout.EAST);

        DetectiveGame.setDarkTheme(leftBox);
        DetectiveGame.setDarkTheme(rightBox);
        DetectiveGame.setDarkTheme(btnExit);

        add(topPanel, BorderLayout.NORTH);

        // 나가기 버튼 동작
        btnExit.addActionListener(e -> {
            if (gameTimer.isRunning()) gameTimer.stop();
            currentInput = "";
            display.setText("");
            wrongAttempts = 0; // 틀린 횟수도 리셋
            gameManager.onMiniGameComplete(false);
        });
        
        // 상단 중앙: 디스플레이 + 힌트
        JPanel centerPanel = new JPanel(new GridBagLayout()); // (신규) 힌트 추가를 위해
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        display = new JTextField(4);
        display.setFont(new Font("Monospaced", Font.BOLD, 48));
        display.setEditable(false);
        display.setHorizontalAlignment(JTextField.CENTER);
        display.setPreferredSize(new Dimension(200, 80));
        gbc.gridy = 0; centerPanel.add(display, gbc);
        // 추가된 힌트 표시
        lblHint = new JLabel("힌트: ");
        lblHint.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblHint.setForeground(Color.CYAN);
        gbc.gridy = 1; centerPanel.add(lblHint, gbc);

        add(centerPanel, BorderLayout.CENTER);
        // 숫자 키패드 (1~9, Cancel, 0, Enter)
        JPanel keypad = new JPanel(new GridLayout(4, 3, 5, 5));
        keypad.setBorder(BorderFactory.createEmptyBorder(10, 200, 10, 200));
        for (int i = 1; i <= 9; i++) { keypad.add(createKeypadButton(String.valueOf(i))); }
        keypad.add(createKeypadButton("Cancel")); keypad.add(createKeypadButton("0")); keypad.add(createKeypadButton("Enter"));
        add(keypad, BorderLayout.SOUTH);
        // 남은 시간 타이머
        gameTimer = new javax.swing.Timer(1000, e -> {
            timeLeft--;
            lblTimer.setText(String.format("남은 시간: %02d초", timeLeft));
            if (timeLeft <= 0) {
                gameTimer.stop();
                GameManager.showThemedMessage("시간 초과!", "실패", JOptionPane.WARNING_MESSAGE);
                gameManager.onMiniGameComplete(false);
            }
        });
        DetectiveGame.setDarkTheme(this);
    }
    // 숫자/Cancel/Enter 공용 버튼 생성
    private JButton createKeypadButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 24));
        btn.addActionListener(e -> onKeypadInput(text));
        if (text.equals("Cancel")) btn.setForeground(Color.RED);
        if (text.equals("Enter")) btn.setForeground(Color.GREEN);
        DetectiveGame.setDarkTheme(btn); // 버튼에도 테마 적용
        return btn;
    }
    // 버튼 입력 처리
    private void onKeypadInput(String input) {
        if (!gameTimer.isRunning()) return;
        if (input.equals("Cancel")) { currentInput = ""; } // 입력 초기화
        else if (input.equals("Enter")) {
            if (currentInput.length() == 4) {
                boolean success = currentGame.checkCode(currentInput);
                if (success) {
                    // 정답일 경우
                    gameTimer.stop();
                    currentInput = "";
                    display.setText("");
                    wrongAttempts = 0;   // 성공 시 실패횟수 초기화
                    gameManager.onMiniGameComplete(true);
                } else {
                    // 틀렸을 경우
                    wrongAttempts++;   // 틀린 횟수 증가
                    int remaining = MAX_ATTEMPTS - wrongAttempts;

                    // 경고창 띄우기 (UIManager 필요 없음, 기존 메시지 방식 사용)
                    GameManager.showThemedMessage(
                            "비밀번호가 틀렸습니다!\n남은 기회: " + remaining,
                            "오답",
                            JOptionPane.ERROR_MESSAGE
                    );

                    currentInput = "";
                    display.setText("");

                    if (wrongAttempts >= MAX_ATTEMPTS) {
                        // 3회 틀리면 바로 실패 처리
                        gameTimer.stop();
                        wrongAttempts = 0; // 다음 게임을 위해 초기화함
                        gameManager.onMiniGameComplete(false);

                    }
                }
            }
        } else { if (currentInput.length() < 4) { currentInput += input; } }
        display.setText(currentInput);
    }
    // GameManager가 미니게임 시작 시 호출
    public void setGame(CodeLockGame game) {
        this.currentGame = game;
        this.currentInput = ""; display.setText("");
        lblHint.setText(game.getHint()); // (신규) 힌트 설정
        this.timeLeft = game.getTimeLimitSeconds();
        lblTimer.setText(String.format("남은 시간: %02d초", timeLeft));
        gameTimer.start();
    }
}

/**
 * (신규) 6: 슬라이딩 퍼즐
 * 3x3 숫자 퍼즐 (0이 빈칸)
 * 인접한 칸과만 교환 가능, 0~8까지 순서대로 맞추면 성공
 */
class SliderPuzzleGamePanel extends JPanel {
    private GameManager gameManager;
    private SliderPuzzleGame currentGame;

    private JPanel puzzlePanel;
    private JButton[] buttons;
    private int emptySlot; // 현재 빈칸 인덱스
    private List<Integer> board; // 현재 퍼즐 상태 리스트

    private JLabel lblTimer;
    private javax.swing.Timer gameTimer;
    private int timeLeft;

    private static final int GRID_SIZE = 3; // 3x3

    public SliderPuzzleGamePanel(GameManager gm) {
        this.gameManager = gm;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder("미니게임: 슬라이딩 퍼즐"));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // 왼쪽: 설명 + 타이머
        JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        leftBox.add(new JLabel("숫자 순서대로 퍼즐을 맞추세요. (빈칸: 0)"));

        lblTimer = new JLabel("남은 시간: 00초");
        lblTimer.setFont(new Font("Monospaced", Font.BOLD, 16));
        lblTimer.setForeground(Color.RED);
        leftBox.add(lblTimer);

        // 오른쪽: 나가기 버튼
        JButton btnExit = new JButton("나가기");
        btnExit.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightBox.add(btnExit);

        topPanel.add(leftBox, BorderLayout.WEST);
        topPanel.add(rightBox, BorderLayout.EAST);

        DetectiveGame.setDarkTheme(leftBox);
        DetectiveGame.setDarkTheme(rightBox);
        DetectiveGame.setDarkTheme(btnExit);

        add(topPanel, BorderLayout.NORTH);

        // 나가기 버튼 동작
        btnExit.addActionListener(e -> {
            if (gameTimer.isRunning()) gameTimer.stop();
            gameManager.onMiniGameComplete(false);
        });

        puzzlePanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE, 5, 5));
        puzzlePanel.setBorder(BorderFactory.createEmptyBorder(20, 100, 20, 100));
        buttons = new JButton[GRID_SIZE * GRID_SIZE];
        // 9개의 버튼 생성
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new JButton();
            buttons[i].setFont(new Font("SansSerif", Font.BOLD, 36));
            buttons[i].setPreferredSize(new Dimension(100, 100));
            final int index = i;
            buttons[i].addActionListener(e -> onTileClick(index));
            puzzlePanel.add(buttons[i]);
        }
        add(puzzlePanel, BorderLayout.CENTER);
        // 남은 시간 타이머
        gameTimer = new javax.swing.Timer(1000, e -> {
            timeLeft--;
            lblTimer.setText(String.format("남은 시간: %02d초", timeLeft));
            if (timeLeft <= 0) {
                gameTimer.stop();
                GameManager.showThemedMessage("시간 초과!", "실패", JOptionPane.WARNING_MESSAGE);
                gameManager.onMiniGameComplete(false);
            }
        });
        DetectiveGame.setDarkTheme(this);
    }
    // GameManager가 미니게임 시작 시 호출
    public void setGame(SliderPuzzleGame game) {
        this.currentGame = game;
        // 항상 풀 수 있는 상태로 섞인 보드
        this.board = game.getShuffledBoard();
        updateBoardUI();

        this.timeLeft = game.getTimeLimitSeconds();
        lblTimer.setText(String.format("남은 시간: %02d초", timeLeft));
        gameTimer.start();
    }
    // 타일 클릭 시 처리
    private void onTileClick(int index) {
        if (!gameTimer.isRunning()) return;

        // 빈 칸(emptySlot)과 클릭한 칸(index)이 인접해있는지 확인
        if (isAdjacent(index, emptySlot)) {
            // Swap
            Collections.swap(board, index, emptySlot);
            updateBoardUI();

            // 승리 조건 확인
            if (currentGame.checkWin(board)) {
                gameTimer.stop();
                gameManager.onMiniGameComplete(true);
            }
        }
    }
    // 현재 보드 상태를 버튼 UI와 동기화
    private void updateBoardUI() {
        for (int i = 0; i < board.size(); i++) {
            int num = board.get(i);
            if (num == 0) { // 0이 빈 칸
                buttons[i].setText("");
                buttons[i].setVisible(false);
                emptySlot = i;
            } else {
                buttons[i].setText(String.valueOf(num));
                buttons[i].setVisible(true);
            }
            DetectiveGame.setDarkTheme(buttons[i]); // 버튼 테마 적용
        }
    }
    // 두 인덱스가 상하좌우로 인접해 있는지 (거리가 1인지) 확인
    private boolean isAdjacent(int i1, int i2) {
        int r1 = i1 / GRID_SIZE, c1 = i1 % GRID_SIZE;
        int r2 = i2 / GRID_SIZE, c2 = i2 % GRID_SIZE;
        // 거리가 1이면 인접
        return Math.abs(r1 - r2) + Math.abs(c1 - c2) == 1;
    }
}

/**
 * 7: 악성 팝업 차단 게임
 * 화면 여기저기 랜덤으로 뜨는 팝업을 X 버튼으로 닫아서 점수 달성
 * 목표 개수 이상 닫으면 성공
 */
class PopupCloseGamePanel extends JPanel {

    private GameManager gameManager;
    private PopupCloseGame currentGame;

    private JLabel lblTimer, lblScore;
    private javax.swing.Timer gameTimer;   // 게임 시간
    private javax.swing.Timer spawnTimer;  // 팝업 생성 타이머
    private int timeLeft;

    private int score = 0;
    private Random random = new Random();

    // 현재 떠있는 팝업들을 저장
    private List<JPanel> popups = new ArrayList<>();

    public PopupCloseGamePanel(GameManager gm) {
        this.gameManager = gm;

        setLayout(null); // null 레이아웃: 팝업 패널을 절대 좌표로 배치하기 위함
        setBorder(BorderFactory.createTitledBorder("미니게임: 악성 팝업 차단"));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBounds(0, 0, 800, 50);
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // 왼쪽: 점수 + 타이머
        JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        lblScore = new JLabel("차단: 0 / 0");
        lblScore.setFont(new Font("Monospaced", Font.BOLD, 16));
        leftBox.add(lblScore);

        lblTimer = new JLabel("남은 시간: 00초");
        lblTimer.setFont(new Font("Monospaced", Font.BOLD, 16));
        lblTimer.setForeground(Color.RED);
        leftBox.add(lblTimer);

        // 오른쪽: 나가기 버튼
        JButton btnExit = new JButton("나가기");
        btnExit.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightBox.add(btnExit);

        topPanel.add(leftBox, BorderLayout.WEST);
        topPanel.add(rightBox, BorderLayout.EAST);

        add(topPanel);

        DetectiveGame.setDarkTheme(topPanel);
        DetectiveGame.setDarkTheme(leftBox);
        DetectiveGame.setDarkTheme(rightBox);
        DetectiveGame.setDarkTheme(btnExit);
        
        // 나가기 버튼 동작
        btnExit.addActionListener(e -> {
            if (timeLeft > 0) { // 게임 중일 때만 동작하게 만듬
                endGameAsFail();
            }
        });

        // 게임 시간 타이머
        gameTimer = new javax.swing.Timer(1000, e -> {
            timeLeft--;
            lblTimer.setText(String.format("남은 시간: %02d초", timeLeft));

            if (timeLeft <= 0) {
                endGame();
            }
        });

        // 팝업 생성 타이머, 0.6초마다 팝업 생성
        spawnTimer = new javax.swing.Timer(600, e -> spawnPopup());

        DetectiveGame.setDarkTheme(this);
    }
    
    // 팝업 생성 함수
    private void spawnPopup() {
        JPanel popup = new JPanel(null);
        popup.setBackground(DetectiveGame.BG_COLOR);
        popup.setBorder(BorderFactory.createLineBorder(DetectiveGame.BTN_BORDER_COLOR, 2));

        // 팝업 크기 랜덤 (3단계로 차이 나게)
        int sizeMode = random.nextInt(3); // 0=작게, 1=중간, 2=크게
        int w, h;

        if (sizeMode == 0) {
            // 1단계(작게)
            w = 60 + random.nextInt(50);    // 60~110
            h = 50 + random.nextInt(40);    // 50~90
        }
        else if (sizeMode == 2) {
            // 3단계(크게)
            w = 220 + random.nextInt(120);  // 220~340
            h = 180 + random.nextInt(120);  // 180~300
        }
        else {
            // 2단계(중간)
            w = 120 + random.nextInt(80);   // 120~200
            h = 100 + random.nextInt(80);   // 100~180
        }

        // 패널 크기 맞춰 팝업 위치 랜덤
        int x = random.nextInt(Math.max(1, getWidth() - w - 20));
        int y = 50 + random.nextInt(Math.max(1, getHeight() - h - 50));

        popup.setBounds(x, y, w, h);

        // X 버튼 크기도 팝업 크기에 비례하도록 설정한다
        int closeW = Math.max(12, w / 8);
        int closeH = Math.max(12, h / 8);

        JButton btnClose = new JButton("X");
        btnClose.setFont(new Font("SansSerif", Font.BOLD, closeH)); // 글자 크기도 비례
        btnClose.setForeground(DetectiveGame.FG_COLOR);
        btnClose.setBackground(DetectiveGame.BTN_BG_COLOR);
        btnClose.setBorder(BorderFactory.createLineBorder(DetectiveGame.BTN_BORDER_COLOR));
        btnClose.setBounds(w - 40, 5, 35, 25);

        // 팝업 오른쪽 상단에 X 버튼 배치
        btnClose.setBounds(w - closeW - 5, 5, closeW, closeH);

        // 흰색 포커스 테두리 제거 및 배경 표시 설정함
        btnClose.setFocusPainted(false);
        btnClose.setContentAreaFilled(false);
        btnClose.setOpaque(true);

        btnClose.addActionListener(e -> {
            if (!gameTimer.isRunning()) return;

            score++;
            lblScore.setText(String.format("차단: %d / %d", score, currentGame.getTargetScore()));

            popup.setVisible(false);
            remove(popup);
            popups.remove(popup);
            repaint();
            // 목표 수치에 도달하면 게임 종료
            if (score >= currentGame.getTargetScore()) {
                endGame();
            }
        });

        popup.add(btnClose);
        add(popup);
        popups.add(popup);
        repaint();
    }


    // 게임 종료 처리
    private void endGame() {
        gameTimer.stop();
        spawnTimer.stop();

        // 남아있는 팝업 제거
        for (JPanel p : popups) remove(p);
        popups.clear();

        boolean success = (score >= currentGame.getTargetScore());
        gameManager.onMiniGameComplete(success);
    }
    
    private void endGameAsFail() {
        gameTimer.stop();
        spawnTimer.stop();

        for (JPanel p : popups) remove(p);
        popups.clear();

        gameManager.onMiniGameComplete(false); // 무조건 실패 처리
    }

    // GameManager가 미니게임 시작 시 호출
    public void setGame(PopupCloseGame game) {
        this.currentGame = game;
        this.score = 0;

        lblScore.setText(String.format("차단: %d / %d", score, game.getTargetScore()));

        this.timeLeft = game.getTimeLimitSeconds();
        lblTimer.setText(String.format("남은 시간: %02d초", timeLeft));

        // 이전 팝업들 제거
        for (JPanel p : popups) remove(p);
        popups.clear();

        gameTimer.start();
        spawnTimer.start();
    }
}

/**
 * 게임 종료 화면
 * 승/패 메시지와 다시 시작 버튼 제공
 */
class EndGamePanel extends JPanel {
    private GameManager gameManager;
    private JLabel lblResult;

    public EndGamePanel(GameManager gm) {
        this.gameManager = gm;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);

        lblResult = new JLabel("게임 결과");
        lblResult.setFont(new Font("SansSerif", Font.BOLD, 28));
        gbc.gridy = 0; add(lblResult, gbc);

        JButton btnRestart = new JButton("다시 시작하기");
        btnRestart.setFont(new Font("SansSerif", Font.BOLD, 20));
        gbc.gridy = 1; add(btnRestart, gbc);

        // 다시 시작 시 GameManager 상태 초기화 + 메인 화면으로
        btnRestart.addActionListener(e -> {
            gameManager.startGame();
            ((DetectiveGame) SwingUtilities.getWindowAncestor(this)).showScreen("MAIN_GAME");
        });
        DetectiveGame.setDarkTheme(this);
    }

    public void setResult(boolean isWin, String message) {
        String htmlMessage = "<html><div style='text-align:center;'>"
                + message.replace("\n", "<br>")
                + "</div></html>";

        lblResult.setText(htmlMessage);
        lblResult.setForeground(isWin ? DetectiveGame.FG_COLOR : Color.RED);
    }
}

// Start Screen Panel (게임 시작 화면)
/**
 * 시작 화면
 * 글리치 애니메이션 타이틀
 * START / HOW TO PLAY 버튼 제공
 */
class StartScreenPanel extends JPanel {

    private final Color NEON = new Color(0, 255, 102);

    public StartScreenPanel(DetectiveGame game) {

        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        // 글리치 타이머를 담을 래퍼 (람다에서 쓰려고 배열 사용함)
        final javax.swing.Timer[] glitchTimer = new javax.swing.Timer[1];
        final javax.swing.Timer[] fixTimer = new javax.swing.Timer[1];

        // 중앙 요소 (GridBagLayout으로 가운데 정렬)
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(Color.BLACK);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel center = new JPanel();
        center.setBackground(Color.BLACK);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        // 타이틀 라벨
        JLabel title = new JLabel("IP.107.SECURE");
        title.setFont(new Font("Consolas", Font.BOLD, 72));
        title.setForeground(NEON);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(title);

        center.add(Box.createVerticalStrut(25));

        // 글리치 애니메이션 문자열들(글자 한 글자씩 순차적으로 고정)
        String finalText = "IP.107.SECURE";
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.#$%@";

        final int[] fixedCount = {0}; // 왼쪽부터 몇 글자 고정됐는지 카운트 한다

        // 랜덤 글자 뿌리는 타이머
        glitchTimer[0] = new javax.swing.Timer(40, e -> {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < finalText.length(); i++) {
                if (i < fixedCount[0]) {
                    // 이미 고정된 글자
                    sb.append(finalText.charAt(i));
                } else {
                    // 아직 안 고정된 글자 → 랜덤
                    sb.append(chars.charAt((int) (Math.random() * chars.length())));
                }
            }
            title.setText(sb.toString());
        });

        // 한 글자씩 고정하는 타이머
        fixTimer[0] = new javax.swing.Timer(200, e -> {
            fixedCount[0]++;

            if (fixedCount[0] >= finalText.length()) {
                // 전부 고정되면 타이머 종료
                glitchTimer[0].stop();
                fixTimer[0].stop();
                title.setText(finalText);
            }
        });

        // 타이머 시작
        glitchTimer[0].start();
        fixTimer[0].start();

        // 서브 타이틀
        JLabel subtitle = new JLabel("107호의 보안을 사수 및 범인을 검거하라!");
        subtitle.setFont(new Font("Malgun Gothic", Font.PLAIN, 24));
        subtitle.setForeground(NEON);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(subtitle);

        center.add(Box.createVerticalStrut(60));

        // START 버튼
        JButton btnStart = new JButton("START");
        btnStart.setFont(new Font("Consolas", Font.BOLD, 30));
        btnStart.setForeground(NEON);
        btnStart.setBackground(Color.BLACK);
        btnStart.setBorder(BorderFactory.createLineBorder(NEON, 3));
        btnStart.setPreferredSize(new Dimension(300, 80));
        btnStart.setMaximumSize(new Dimension(300, 80));
        btnStart.setFocusPainted(false);
        btnStart.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(btnStart);

        // START 버튼 Hover 효과
        btnStart.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnStart.setBackground(NEON);
                btnStart.setForeground(Color.BLACK);
                btnStart.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnStart.setBackground(Color.BLACK);
                btnStart.setForeground(NEON);
                btnStart.setBorder(BorderFactory.createLineBorder(NEON, 3));
            }
        });

        centerWrapper.add(center, gbc);
        add(centerWrapper, BorderLayout.CENTER);

        // HOW TO PLAY 버튼 (오른쪽 아래 고정)
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(Color.BLACK);

        JPanel bottomRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 40, 20));
        bottomRight.setBackground(Color.BLACK);

        JButton btnHow = new JButton("HOW TO PLAY");
        btnHow.setFont(new Font("Consolas", Font.BOLD, 18));
        btnHow.setForeground(NEON);
        btnHow.setBackground(Color.BLACK);
        btnHow.setBorder(BorderFactory.createLineBorder(NEON, 2));
        btnHow.setPreferredSize(new Dimension(200, 45));
        btnHow.setFocusPainted(false);

        // HOW TO PLAY Hover 색상: 마우스 올리면 회색으로 변경
        Color howNormalBg = Color.BLACK;
        Color howHoverBg  = Color.DARK_GRAY;
        btnHow.setBackground(howNormalBg);

        btnHow.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnHow.setBackground(howHoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnHow.setBackground(howNormalBg);
            }
        });
        
        bottomRight.add(btnHow);
        bottomBar.add(bottomRight, BorderLayout.SOUTH);

        add(bottomBar, BorderLayout.SOUTH);

        // 버튼 이벤트: 화면 전환
        btnStart.addActionListener(e -> game.startNewGameFromMenu());
        btnHow.addActionListener(e -> game.showScreen("HOW_TO_PLAY"));
    }
}


// How To Play Panel
/**
 * HOW TO PLAY 화면
 * 게임 전체 규칙과 미니게임 종류를 설명
 */
class HowToPlayPanel extends JPanel {
    public HowToPlayPanel(DetectiveGame game) {
        setLayout(new BorderLayout(10, 10));
        setBackground(DetectiveGame.BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("HOW TO PLAY", SwingConstants.CENTER);
        title.setFont(new Font("Monospaced", Font.BOLD, 36));
        title.setForeground(DetectiveGame.FG_COLOR);
        add(title, BorderLayout.NORTH);

        JTextArea guide = new JTextArea();
        guide.setEditable(false);
        guide.setFont(new Font("Monospaced", Font.PLAIN, 16));
        guide.setForeground(DetectiveGame.FG_COLOR);
        guide.setBackground(DetectiveGame.BG_COLOR);

        // 간단한 룰 설명
        guide.setText(
                "▶ 게임 목표\n"
                        + "- 제한 시간 안에 모든 미니게임을 클리어하세요.\n\n"
                        + "▶ 미니게임 종류\n"
                        + "1) Typing\n2) Wire\n3) Matching\n4) Sequence\n5) Code Lock\n6) Slider\n7) Popup\n\n"
                        + "▶ 성공하면 단서 획득, 실패하면 경보 상승!"
        );

        add(new JScrollPane(guide), BorderLayout.CENTER);

        JButton btnBack = new JButton("BACK");
        btnBack.setFont(new Font("Monospaced", Font.BOLD, 16));
        btnBack.setForeground(DetectiveGame.FG_COLOR);
        btnBack.setBackground(DetectiveGame.BTN_BG_COLOR);
        btnBack.setBorder(BorderFactory.createLineBorder(DetectiveGame.BTN_BORDER_COLOR, 2));
        btnBack.setFocusPainted(false);
        btnBack.addActionListener(e -> game.showScreen("START_SCREEN"));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setBackground(DetectiveGame.BG_COLOR);
        bottom.add(btnBack);
        add(bottom, BorderLayout.SOUTH);

        DetectiveGame.setDarkTheme(this);
    }
}

// Data Models (데이터 객체)
/**
 * 단서(Clue) 클래스
 * id: 조각 값 (예: "107", "192", "FAKE" 등)
 * content: 화면에 보여줄 텍스트 설명함
 */
class Clue {

    public enum Type {
        CORE,      // IP 각 자리의 ‘정답’을 직접 알 수 있는 핵심 단서
        SUPPORT,   // 있으면 검산/추리에 도움 되는 보조 단서
        FAKE       // 일부러 헷갈리게 하는 가짜 단서
    }

    private String id;
    private String content;
    private Type type;

    public Clue(String id, String content, Type type) {
        this.id = id;
        this.content = content;
        this.type = type;
    }

    public String getId() { return id; }
    public String getContent() { return content; }
    public Type getType() { return type; }

    public boolean isCore()    { return type == Type.CORE; }
    public boolean isSupport() { return type == Type.SUPPORT; }
    public boolean isFake()    { return type == Type.FAKE; }

    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Clue clue = (Clue) obj;
        return id.equals(clue.id) && content.equals(clue.content) && type == clue.type;
    }

    @Override public int hashCode() {
        return java.util.Objects.hash(id, content, type);
    }
}

/**
 * 카드 클래스
 * 하나의 작업(미니게임)을 의미함
 * 제목/난이도/연결된 MiniGame 인스턴스 보유함
 */
class Card {
    private String title;
    private int difficulty;
    private MiniGame miniGame;
    
    private boolean cleared = false;
    
    public Card(String title, int difficulty, MiniGame miniGame) {
        this.title = title;
        this.difficulty = difficulty;
        this.miniGame = miniGame;
    }
    public String getTitle() { return title; }
    public int getDifficulty() { return difficulty; }
    public MiniGame getMiniGame() { return miniGame; }
    
    public boolean isCleared() {
        return cleared;
    }
    
    public void setCleared(boolean cleared) {
        this.cleared = cleared;
    }
}
/**
 * 플레이어 상태
 * 현재까지 수집한 단서 목록 관리
 */
class PlayerState {
    private List<Clue> collectedClues = new ArrayList<>();
    public void addClue(Clue clue) { if (!collectedClues.contains(clue)) { collectedClues.add(clue); } }
    public void clearClues() { this.collectedClues.clear(); }
    public List<Clue> getCollectedClues() { return collectedClues; }
}
/**
 * 최종 IP 정답 체크용 클래스
 */
class ResultChecker {
    private String correctIp;
    public ResultChecker(String ip) { this.correctIp = ip; }
    public boolean checkAnswer(String submittedIp) { return correctIp.equals(submittedIp); }
    public String getCorrectIp() { return correctIp; }
}

// MiniGame Interfaces
/**
 * 모든 미니게임이 공통으로 구현해야 하는 인터페이스
 * 게임 이름
 * 성공 시 제공할 단서
 * 실패 시 경보 페널티
 * 제한 시간 (초)
 */
// [다형성 구현] 모든 미니게임의 공통 규격 인터페이스
interface MiniGame {
    String getGameName();
    Clue getRewardClue();
    int getPenalty();
    int getTimeLimitSeconds();
}

// 타이핑
class TypingMiniGame implements MiniGame {
    private String targetText; private int timeLimit; private Clue reward; private int penalty;
    public TypingMiniGame(String t, int ti, Clue r, int p) { targetText = t; timeLimit = ti; reward = r; penalty = p; }
    public String getTargetText() { return targetText; }
    public boolean checkAnswer(String i) { return targetText.equals(i); }
    @Override public String getGameName() { return "타이핑 게임"; }
    @Override public Clue getRewardClue() { return reward; }
    @Override public int getPenalty() { return penalty; }
    @Override public int getTimeLimitSeconds() { return timeLimit; }
}

// 와이어 연결
class WireConnectingGame implements MiniGame {
    private int timeLimit; private Clue reward; private int penalty;
    // 사용할 와이어 색상 4개
    private List<Color> colors = List.of(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW);
    public WireConnectingGame(int ti, Clue r, int p) { timeLimit = ti; reward = r; penalty = p; }
    public List<Color> getColors() { return colors; }
    @Override public String getGameName() { return "와이어 연결"; }
    @Override public Clue getRewardClue() { return reward; }
    @Override public int getPenalty() { return penalty; }
    @Override public int getTimeLimitSeconds() { return timeLimit; }
}

// 카드 짝 맞추기
class CardMatchingGame implements MiniGame {
    private int timeLimit; private Clue reward; private int penalty;
    private final int totalPairs = 6; // 총 6쌍
    public CardMatchingGame(int ti, Clue r, int p) { timeLimit = ti; reward = r; penalty = p; }
    // 6가지 기호를 2개씩 넣고 랜덤 섞어서 반환
    public List<String> getShuffledSymbols() {
        List<String> s = new ArrayList<>();
        String[] p = {"#", "@", "*", "$", "%", "&"};
        for (String str : p) { s.add(str); s.add(str); }
        Collections.shuffle(s); return s;
    }
    public int getTotalPairs() { return totalPairs; }
    @Override public String getGameName() { return "카드 짝 맞추기"; }
    @Override public Clue getRewardClue() { return reward; }
    @Override public int getPenalty() { return penalty; }
    @Override public int getTimeLimitSeconds() { return timeLimit; }
}

// 순서 기억
class SequenceMemoryGame implements MiniGame {
    private int timeLimit; private Clue reward; private int penalty;

    public SequenceMemoryGame(int ti, Clue r, int p) {
        timeLimit = ti; reward = r; penalty = p;
    }
    // 0,1,2,3이 한 번씩만 등장하도록 섞어서 반환함
    public List<Integer> getSequence() {
        List<Integer> seq = new ArrayList<>();
        for (int i = 0; i < 4; i++) {      // 0,1,2,3
            seq.add(i);
        }
        Collections.shuffle(seq); // 순서만 랜덤
        return seq; // 길이 4, 각 색 1번씩
    }

    @Override public String getGameName() { return "순서 기억"; }
    @Override public Clue getRewardClue() { return reward; }
    @Override public int getPenalty() { return penalty; }
    @Override public int getTimeLimitSeconds() { return timeLimit; }
}


// 코드락
class CodeLockGame implements MiniGame {
    private String correctCode; private int timeLimit; private Clue reward; private int penalty;
    private String hint; // 힌트 텍스트
    public CodeLockGame(String c, int ti, Clue r, int p, String h) {
        correctCode = c; timeLimit = ti; reward = r; penalty = p; hint = h;
    }
    public boolean checkCode(String i) { return correctCode.equals(i); }
    public String getHint() { return hint; } // (신규)
    @Override public String getGameName() { return "코드락 해제"; }
    @Override public Clue getRewardClue() { return reward; }
    @Override public int getPenalty() { return penalty; }
    @Override public int getTimeLimitSeconds() { return timeLimit; }
}

// 슬라이딩 퍼즐
class SliderPuzzleGame implements MiniGame {
    private int timeLimit; private Clue reward; private int penalty;
    private final int size = 9; // 3x3

    public SliderPuzzleGame(int ti, Clue r, int p) { timeLimit = ti; reward = r; penalty = p; }
    // 항상 풀 수 있는 상태가 되도록 보드를 섞어서 반환한다
    public List<Integer> getShuffledBoard() {
        List<Integer> board = new ArrayList<>();
        for (int i = 0; i < size; i++) { board.add(i); } // 0~8 (0이 빈칸)

        // 풀 수 있도록 보장됨
        // 완성된 상태에서 랜덤으로 유효한 이동을 100번 수행한다
        Random rand = new Random();
        int emptySlot = 0;
        for (int i = 0; i < 100; i++) {
            List<Integer> neighbors = new ArrayList<>();
            int r = emptySlot / 3, c = emptySlot % 3;
            if (r > 0) neighbors.add(emptySlot - 3); // Up
            if (r < 2) neighbors.add(emptySlot + 3); // Down
            if (c > 0) neighbors.add(emptySlot - 1); // Left
            if (c < 2) neighbors.add(emptySlot + 1); // Right

            int swapIndex = neighbors.get(rand.nextInt(neighbors.size()));
            Collections.swap(board, emptySlot, swapIndex);
            emptySlot = swapIndex;
        }
        return board;
    }

    // 0~8 순서대로 정렬되어 있는지 체크
    public boolean checkWin(List<Integer> board) {
        for (int i = 0; i < size; i++) {
            if (board.get(i) != i) return false;
        }
        return true;
    }

    @Override public String getGameName() { return "슬라이딩 퍼즐"; }
    @Override public Clue getRewardClue() { return reward; }
    @Override public int getPenalty() { return penalty; }
    @Override public int getTimeLimitSeconds() { return timeLimit; }
}

// 팝업 차단
class PopupCloseGame implements MiniGame {
    private int timeLimit; // 전체 제한 시간
    private int targetScore; // 목표 차단 수
    private Clue reward;
    private int penalty;

    public PopupCloseGame(int ti, int ts, Clue r, int p) {
        timeLimit = ti; targetScore = ts; reward = r; penalty = p;
    }

    public int getTargetScore() { return targetScore; }
    @Override public String getGameName() { return "팝업 차단"; }
    @Override public Clue getRewardClue() { return reward; }
    @Override public int getPenalty() { return penalty; }
    @Override public int getTimeLimitSeconds() { return timeLimit; }
}