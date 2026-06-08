package com.example.hundirlaflota;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.graphics.Canvas;
import android.graphics.Paint;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class MainActivity extends AppCompatActivity {

    enum EstatJoc { ATURAT, JUGANT, EN_ESPERA, ACABAT }
    EstatJoc estatJoc = EstatJoc.ATURAT;
    Jugador torn;
    TextView titol;
    TextView DarreraJugada1;
    TextView DarreraJugada2;
    TextView missatges;

    private GestorWebSocket gestorWebSocket;
    private boolean connectat = false;
    private boolean onlineMode = false;
    int idVaixell = 0;

    SurfaceView taulerIntents;
    SurfaceView taulerVaixells;

    ImageButton atura;
    ImageButton newGame;
    ImageButton online;
    ImageButton tip;

    Set<View> conjuntPistes = new HashSet<>();
    Set<View> conjuntFinalJoc = new HashSet<>();

    ConstraintLayout zonaPistes;
    ConstraintLayout caixaPistes;
    TextView titolMeus, titolRival, textPistesMeus, textPistesRival;
    ImageButton botoTancarPistes;

    ConstraintLayout zonaFinalJoc;
    ConstraintLayout caixaFinalJoc;
    TextView titolFinalJoc;
    ImageButton botoTancarFinalJoc;
    Button botoResum;

    SurfaceView srfcResumLocal, srfcResumRival;

    Casella last_play = null;

    HashMap<Jugador, HashMap<Casella, Vaixell>> vaixellsVius = new HashMap<>();
    HashMap<Jugador, HashMap<Casella, Vaixell>> vaixellsTocats = new HashMap<>();

    HashMap<Jugador, HashMap<Casella, Vaixell>> vaixells = new HashMap<>();

    HashSet<Casella> casellesLocal = new HashSet<>();
    HashSet<Casella> casellesRival = new HashSet<>();

    HashMap<Jugador, TreeMap<Vaixell, TreeSet<Casella>>> mappingVaixellCaselles = new HashMap<>();

    Casella primerImpacteRival = null;
    Casella ultimImpacteRival = null;
    int[] direccioRival = null;
    ArrayList<int[]> direccionsRestants = new ArrayList<>();

    LinkedList<Jugada> historial = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        titol = findViewById(R.id.titol);
        DarreraJugada1 = findViewById(R.id.DarreraJugada1);
        DarreraJugada2 = findViewById(R.id.DarreraJugada2);
        missatges = findViewById(R.id.missatges);

        taulerIntents = findViewById(R.id.taulerIntents);
        taulerVaixells = findViewById(R.id.taulerVaixells);

        atura = findViewById(R.id.atura);
        newGame = findViewById(R.id.newGame);
        online = findViewById(R.id.online);
        tip = findViewById(R.id.tip);
        zonaPistes = findViewById(R.id.zonaPistes);
        caixaPistes = findViewById(R.id.caixaPistes);
        titolMeus = findViewById(R.id.titolMeus);
        titolRival = findViewById(R.id.titolRival);
        textPistesMeus = findViewById(R.id.textPistesMeus);
        textPistesRival = findViewById(R.id.textPistesRival);
        botoTancarPistes = findViewById(R.id.botoTancarPistes);

        zonaFinalJoc = findViewById(R.id.zonaFinalJoc);
        caixaFinalJoc = findViewById(R.id.caixaFinalJoc);
        botoTancarFinalJoc = findViewById(R.id.botoTancarFinalJoc);
        titolFinalJoc = findViewById(R.id.titolFinalJoc);
        srfcResumLocal = findViewById(R.id.FinalJocResumLocal);
        srfcResumRival = findViewById(R.id.FinalJocResumRival);
        botoResum = findViewById(R.id.botoResum);

        missatges.setMovementMethod(new ScrollingMovementMethod());
        textPistesMeus.setMovementMethod(new ScrollingMovementMethod());
        textPistesRival.setMovementMethod(new ScrollingMovementMethod());


        SurfaceHolder.Callback callbackTaulers = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                repintarGraelles();
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}
        };

        SurfaceHolder.Callback callbackResumLocal = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if(srfcResumLocal.getHolder().getSurface().isValid()) {
                    Canvas canvas = srfcResumLocal.getHolder().lockCanvas();
                    if (canvas != null) {
                        Bitmap img = historial.peekLast().jugador == Jugador.LOCAL? BitmapFactory.decodeResource(getResources(), R.drawable.winner) : BitmapFactory.decodeResource(getResources(), R.drawable.loser);
                        canvas.drawColor(Color.parseColor("#ADD8E6"));
                        canvas.drawBitmap(img, new Rect(0,0,img.getWidth(),img.getHeight()), new Rect(0,0,canvas.getWidth(), canvas.getHeight()), null);
                    }
                    srfcResumLocal.getHolder().unlockCanvasAndPost(canvas);
                }
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}
        };

        SurfaceHolder.Callback callbackResumRival = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if(srfcResumRival.getHolder().getSurface().isValid()) {
                    Canvas canvas = srfcResumRival.getHolder().lockCanvas();
                    if (canvas != null) {
                        Bitmap img = historial.peekLast().jugador == Jugador.LOCAL? BitmapFactory.decodeResource(getResources(), R.drawable.winner) : BitmapFactory.decodeResource(getResources(), R.drawable.loser);
                        canvas.drawColor(Color.parseColor("#ADD8E6"));
                        canvas.drawBitmap(img, new Rect(0,0,img.getWidth(),img.getHeight()), new Rect(0,0,canvas.getWidth(), canvas.getHeight()), null);
                    }
                    srfcResumRival.getHolder().unlockCanvasAndPost(canvas);
                }
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}
        };
        taulerVaixells.getHolder().addCallback(callbackTaulers);
        taulerIntents.getHolder().addCallback(callbackTaulers);
        srfcResumLocal.getHolder().addCallback(callbackResumLocal);
        srfcResumRival.getHolder().addCallback(callbackResumRival);

        conjuntPistes.add(zonaPistes);
        conjuntPistes.add(caixaPistes);
        conjuntPistes.add(titolMeus);
        conjuntPistes.add(titolRival);
        conjuntPistes.add(textPistesMeus);
        conjuntPistes.add(textPistesRival);
        conjuntPistes.add(botoTancarPistes);

        conjuntFinalJoc.add(zonaFinalJoc);
        conjuntFinalJoc.add(caixaFinalJoc);
        conjuntFinalJoc.add(botoTancarFinalJoc);
        conjuntFinalJoc.add(titolFinalJoc);
        conjuntFinalJoc.add(botoResum);
        conjuntFinalJoc.add(srfcResumLocal);
        conjuntFinalJoc.add(srfcResumRival);
        mostraPistes(false);

        newGame.setOnClickListener(v -> iniciarPartida());
        online.setOnClickListener(v -> connecta());
        atura.setOnClickListener(v -> {
            if(onlineMode) enviarSortirPartida();
            gestorWebSocket.tancar();
            onlineMode = false;
            estatJoc = EstatJoc.ATURAT;
            actualitzaBotons();
            mostrarMissatge("Has sortit de la partida.");
        });

        tip.setOnClickListener(v -> {
            StringBuilder textVaixells = new StringBuilder("");
            mappingVaixellCaselles.get(Jugador.LOCAL).forEach((vaixell, caselles) -> {
                textVaixells.append(String.format("Vaixell de %s: ", vaixell.mida));
                caselles.forEach((Casella casella) -> {
                    if(vaixellsVius.get(Jugador.LOCAL).containsKey(casella))textVaixells.append(String.format("(%s,%s),", casella.x, casella.y));
                    else textVaixells.append(String.format("<font color=#ff0000>(%s,%s)</font>,", casella.x, casella.y));
                });
                textVaixells.setLength(textVaixells.length()-1);
                textVaixells.append("<br>");
            });
            textPistesMeus.setText(Html.fromHtml(textVaixells.toString()));
            if(!onlineMode) {
                textVaixells.setLength(0);
                mappingVaixellCaselles.get(Jugador.RIVAL).forEach((vaixell, caselles) -> {
                    textVaixells.append(String.format("Vaixell de %s: ", vaixell.mida));
                    caselles.forEach((Casella casella) -> {
                        if (vaixellsVius.get(Jugador.RIVAL).containsKey(casella))
                            textVaixells.append(String.format("(%s,%s),", casella.x, casella.y));
                        else
                            textVaixells.append(String.format("<font color=#ff0000>(%s,%s)</font>,", casella.x, casella.y));
                    });
                    textVaixells.setLength(textVaixells.length() - 1);
                    textVaixells.append("<br>");
                });
                textPistesRival.setText(Html.fromHtml(textVaixells.toString()));
            }
            mostraPistes(true);
        });

        botoTancarPistes.setOnClickListener(v -> mostraPistes(false));
        botoTancarFinalJoc.setOnClickListener(v -> mostraFinalJoc(false));
        botoResum.setOnClickListener(v -> mostraResumJoc());
        gestorWebSocket = new GestorWebSocket(
                new GestorWebSocket.EscoltadorWebSocket() {
            @Override
            public void enConnectar() {
                runOnUiThread(() -> mostrarMissatge("WS: Connectat"));
                connectat = true;
            }

            @Override
            public void enRebreMissatge(JSONObject json) {
                runOnUiThread(() -> gestionarMissatge(json));
            }

            @Override
            public void enDesconnectar() {
                runOnUiThread(() -> {
                    mostrarMissatge("WS: Desconnectat");
                    connectat = false;
                });
            }

            @Override
            public void enError(String error) {
                runOnUiThread(() -> {
                    mostrarMissatge("WS: Error: " + error);

                    onlineMode = false;
                    estatJoc = EstatJoc.ATURAT;
                    actualitzaBotons();
                });
            }
        });

        actualitzaBotons();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void actualitzaBotons() {
        if (estatJoc == EstatJoc.ATURAT || estatJoc == EstatJoc.ACABAT) {
            newGame.setEnabled(true);
            online.setEnabled(true);
            atura.setEnabled(false);
            tip.setEnabled(false);
        } else {
            newGame.setEnabled(false);
            online.setEnabled(false);
            atura.setEnabled(true);
            tip.setEnabled(true);
        }
    }

    public void pintar(SurfaceView tauler, int files, int columnes, Canvas canvas){
        int alt = tauler.getHeight();
        int ampla = tauler.getWidth();
        canvas.drawColor(Color.parseColor("#ADD8E6"));
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        p.setStrokeWidth(3);
        float midaX = ampla/(float) columnes;
        float midaY = alt/(float) files;

        for(int i = 0; i<=columnes; i++){
            float x = i*midaX;
            canvas.drawLine(x,0,x,alt,p);
        }
        for(int j = 0; j<=files; j++){
            float y = j*midaY;
            canvas.drawLine(0,y,ampla,y,p);
        }

        Jugador jugador = (tauler == taulerVaixells) ? Jugador.LOCAL : Jugador.RIVAL;
        HashSet<Casella> atacsRival = (jugador == Jugador.LOCAL) ? casellesRival : casellesLocal;

        if (jugador == Jugador.LOCAL && vaixellsVius.containsKey(jugador)) {
            HashMap<Casella, Vaixell> mapVius = vaixellsVius.get(jugador);
            for (Casella casella : mapVius.keySet()) {
                Vaixell vaixell = mapVius.get(casella);
                Paint pRect = new Paint();
                pRect.setStyle(Paint.Style.FILL);
                pRect.setColor(vaixell.color);
                canvas.drawRoundRect(casella.x * midaX + 2, casella.y * midaY + 2, (casella.x + 1) * midaX -2, (casella.y + 1) * midaY -2, 20, 20, pRect);
            }
        }

        Paint pAigua = new Paint();
        pAigua.setColor(Color.WHITE);
        pAigua.setAlpha(180);

        Paint pTocat = new Paint();
        pTocat.setColor(Color.RED);
        pTocat.setAlpha(200);

        Paint pPunt = new Paint();
        pPunt.setColor(Color.BLACK);
        HashMap<Casella, Vaixell> tocats = vaixellsTocats.get(jugador);

        for (Casella c : atacsRival) {
            Paint pCurrent = (tocats != null && tocats.containsKey(c)) ? pTocat : pAigua;
            canvas.drawRoundRect(c.x * midaX + 2, c.y * midaY + 2, (c.x + 1) * midaX -2, (c.y + 1) * midaY -2, 20, 20, pCurrent);
            canvas.drawCircle((c.x * midaX) + (midaX / 2), (c.y * midaY) + (midaY / 2), midaX / 6, pPunt);
        }
    }

    public void pintar(SurfaceView tauler, int files, int columnes){
        if(tauler.getHolder().getSurface().isValid()){
            Canvas canvas = tauler.getHolder().lockCanvas();
            if (canvas != null) {
                pintar(tauler, files, columnes, canvas);
                tauler.getHolder().unlockCanvasAndPost(canvas);
            }

        }
    }

    private void repintarGraelles() {
        pintar(taulerVaixells,10,10);
        pintar(taulerIntents,10,10);
    }

    private void pintarJugadaResum(Jugada j){
        Jugador defensor = (j.jugador == Jugador.LOCAL) ? Jugador.RIVAL : Jugador.LOCAL;
        SurfaceView srfc = (defensor == Jugador.LOCAL) ? srfcResumLocal : srfcResumRival;

        Canvas canvas = srfc.getHolder().lockCanvas();

        canvas.drawColor(Color.parseColor("#ADD8E6"));
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        p.setStrokeWidth(3);
        int alt = srfc.getHeight();
        int ampla = srfc.getWidth();
        float midaX = ampla/(float) 10;
        float midaY = alt/(float) 10;

        for(int i = 0; i<=10; i++){
            float x = i*midaX;
            canvas.drawLine(x,0,x,alt,p);
        }

        for(int k = 0; k<=10; k++){
            float y = k*midaY;
            canvas.drawLine(0,y,ampla,y,p);
        }


        HashMap<Casella, Vaixell> mapCasellesVaixell = vaixells.get(defensor);
        for (Casella casella : mapCasellesVaixell.keySet()) {
            Vaixell vaixell = mapCasellesVaixell.get(casella);
            Paint pRect = new Paint();
            pRect.setStyle(Paint.Style.FILL);
            pRect.setColor(vaixell.color);
            canvas.drawRoundRect(casella.x * midaX + 2, casella.y * midaY + 2, (casella.x + 1) * midaX -2, (casella.y + 1) * midaY -2, 20, 20, pRect);
        }


        HashSet<Casella> atacsRebuts = (defensor == Jugador.LOCAL) ? casellesRival : casellesLocal;

        Paint pAigua = new Paint();
        pAigua.setColor(Color.WHITE);
        pAigua.setAlpha(180);

        Paint pTocat = new Paint();
        pTocat.setColor(Color.RED);
        pTocat.setAlpha(200);

        Paint pPunt = new Paint();
        pPunt.setColor(Color.BLACK);

        HashMap<Casella, Vaixell> tocats = vaixellsTocats.get(defensor);

        for (Casella c : atacsRebuts) {
            Paint pCurrent = (tocats != null && tocats.containsKey(c)) ? pTocat : pAigua;
            canvas.drawRoundRect(c.x * midaX + 2, c.y * midaY + 2, (c.x + 1) * midaX -2, (c.y + 1) * midaY -2, 20, 20, pCurrent);
            canvas.drawCircle((c.x * midaX) + (midaX / 2), (c.y * midaY) + (midaY / 2), midaX / 6, pPunt);
        }

        srfc.getHolder().unlockCanvasAndPost(canvas);
    }

    private void mostraResumJoc(){
        vaixellsVius.clear();
        casellesRival.clear();
        casellesLocal.clear();
        vaixellsVius = (HashMap<Jugador, HashMap<Casella, Vaixell>>)vaixells.clone();
        vaixellsTocats.clear();
        vaixellsTocats.put(Jugador.LOCAL, new HashMap<>());
        vaixellsTocats.put(Jugador.RIVAL, new HashMap<>());
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable[] task = new Runnable[1];
        Jugada jPintarGraelles = new Jugada(Jugador.LOCAL, new Casella(-1, -1));
        pintarJugadaResum(jPintarGraelles);
        jPintarGraelles.jugador = Jugador.RIVAL;
        pintarJugadaResum(jPintarGraelles);
        task[0] = new Runnable() {
            @Override
            public void run() {
                if (!historial.isEmpty()) {
                    Jugada j = historial.remove();
                    HashSet caselles = j.jugador == Jugador.LOCAL?casellesLocal:casellesRival;
                    Jugador defensor = j.jugador == Jugador.LOCAL?Jugador.RIVAL:Jugador.LOCAL;
                    if(vaixellsVius.get(defensor).containsKey(j.casella)){
                        Vaixell v = vaixellsVius.get(defensor).remove(j.casella);
                        vaixellsTocats.get(defensor).put(j.casella, v);
                    }
                    caselles.add(j.casella);
                    pintarJugadaResum(j);
                    Log.d("JUGADA", String.format("Pintant jugada: jugador %s tira a (%s, %s)", j.jugador, j.casella.x, j.casella.y));
                    handler.postDelayed(this, 200);
                }
            }
        };
        handler.post(task[0]);
    }
    private Casella getCasella(float x, float y){
        int posX = (int)Math.floor(((x - taulerIntents.getX())/taulerIntents.getWidth())*10);
        int posY = (int)Math.floor(((y - taulerIntents.getY())/taulerIntents.getHeight())*10);
        posY = Math.min(posY, 9);

        DarreraJugada2.setText(String.format("Seleccionada la casella (%s, %s)", posX, posY));
        return new Casella(posX, posY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (estatJoc != EstatJoc.JUGANT || torn != Jugador.LOCAL) return true;

        if(
                event.getX()>=taulerIntents.getX() &&
                        event.getX()<=taulerIntents.getX()+taulerIntents.getWidth() &&
                        event.getY()>=taulerIntents.getY() &&
                        event.getY()<=taulerIntents.getY()+taulerIntents.getHeight()
        ){
            if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                last_play = getCasella(event.getX(), event.getY());

                if (!casellesLocal.contains(last_play)) {
                    if(onlineMode) enviarTirada(last_play);
                    else processarJugada(last_play, Jugador.LOCAL);
                }
            }
        }
        return true;
    }

    private void enviarTirada(Casella c){
        try{
            JSONObject json = new JSONObject();
            json.put("tipus","tirar");
            json.put("fila",c.y);
            json.put("columna",c.x);
            gestorWebSocket.enviar(json);
        } catch(JSONException e){
            mostrarMissatge("Error enviant tirada: " + e.getMessage());
        }
    }

    private void scroll() {
        missatges.post(() -> {
            if (missatges.getLayout() != null) {
                int scrollAmount = missatges.getLayout().getLineTop(missatges.getLineCount()) - missatges.getHeight();
                missatges.scrollTo(0, Math.max(scrollAmount, 0));
            }
        });
    }

    private void mostraPistes(boolean mostrar) {
        for (View v : conjuntPistes) {
            v.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        }
    }

    private void mostraFinalJoc(boolean mostrar) {
        for (View v : conjuntFinalJoc) {
            v.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        }
    }

    private void iniciarPartida() {
        vaixellsVius.clear();
        vaixellsTocats.clear();
        casellesLocal.clear();
        casellesRival.clear();
        historial.clear();
        vaixells.clear();
        vaixells.clear();
        mappingVaixellCaselles.clear();
        idVaixell = 0;

        vaixellsTocats.put(Jugador.LOCAL, new HashMap<>());
        vaixellsTocats.put(Jugador.RIVAL, new HashMap<>());

        primerImpacteRival = null;
        ultimImpacteRival = null;
        direccioRival = null;
        direccionsRestants.clear();

        last_play = null;

        crearVaixells(Jugador.LOCAL);
        crearVaixells(Jugador.RIVAL);

        Random r = new Random();
        torn = r.nextBoolean() ? Jugador.LOCAL : Jugador.RIVAL;

        estatJoc = (torn == Jugador.LOCAL) ? EstatJoc.JUGANT : EstatJoc.EN_ESPERA;

        missatges.setText("Nou joc iniciat.\nComença: " + (torn == Jugador.LOCAL ? "Tu" : "Rival"));
        DarreraJugada1.setText("---");
        DarreraJugada2.setText("---");

        actualitzaBotons();
        repintarGraelles();

        if (torn == Jugador.RIVAL) {
            taulerIntents.postDelayed(this::ferJugadaRobot, 500);
        }
    }

    private void processarJugada(Casella c, Jugador atacant) {
        Jugador defensor = (atacant == Jugador.LOCAL) ? Jugador.RIVAL : Jugador.LOCAL;
        HashSet<Casella> destapades = (atacant == Jugador.LOCAL) ? casellesLocal : casellesRival;
        if (destapades.contains(c)) return;
        destapades.add(c);
        historial.add(new Jugada(atacant, c));

        HashMap<Casella, Vaixell> vius = vaixellsVius.get(defensor);
        HashMap<Casella, Vaixell> tocats = vaixellsTocats.get(defensor);
        if (tocats == null) {
            tocats = new HashMap<>();
            vaixellsTocats.put(defensor, tocats);
        }

        boolean encert = false;
        String textTipus = "Aigua";

        if (vius != null) {
            if (vius.containsKey(c)) {
                Vaixell v = vius.get(c);
                vius.remove(c);
                tocats.put(c, v);
                v.tocat++;
                encert = true;

                if (v.tocat == v.mida) {
                    textTipus = "Enfonsat!";
                } else {
                    textTipus = "Tocat!";
                }
            }
        }

        if (atacant == Jugador.RIVAL) {
            if (encert) {
                if (textTipus.equals("Enfonsat!")) {
                    primerImpacteRival = null;
                    ultimImpacteRival = null;
                    direccioRival = null;
                    direccionsRestants.clear();
                } else {
                    if (primerImpacteRival == null) {
                        primerImpacteRival = c;
                        ultimImpacteRival = c;
                        direccionsRestants.add(new int[]{1, 0});
                        direccionsRestants.add(new int[]{-1, 0});
                        direccionsRestants.add(new int[]{0, 1});
                        direccionsRestants.add(new int[]{0, -1});
                        Collections.shuffle(direccionsRestants);
                    } else {
                        direccioRival = new int[]{c.x - ultimImpacteRival.x, c.y - ultimImpacteRival.y};
                        ultimImpacteRival = c;
                    }
                }
            } else {
                if (direccioRival != null) {
                    direccioRival = new int[]{-direccioRival[0], -direccioRival[1]};
                    ultimImpacteRival = primerImpacteRival;
                }
            }
        }

        String textAtac = atacant == Jugador.LOCAL ? "Tu ataques a " : "Rival ataca a ";
        String textFinal = textAtac + "(" + c.x + "," + c.y + "): " + textTipus;
        missatges.append("\n" + textFinal);

        scroll();

        if (atacant == Jugador.LOCAL) DarreraJugada1.setText(textFinal);
        else DarreraJugada2.setText(textFinal);

        repintarGraelles();

        if (vius == null || vius.isEmpty()) {
            estatJoc = EstatJoc.ACABAT;
            actualitzaBotons();
            missatges.append("\n\nPARTIDA ACABADA. Guanya: " + (atacant == Jugador.LOCAL ? "Tu!" : "El Rival!"));
            scroll();
            mostraFinalJoc(true);
            return;
        }

        if (!encert) {
            torn = defensor;
            if (torn == Jugador.LOCAL) {
                estatJoc = EstatJoc.JUGANT;
            } else {
                estatJoc = EstatJoc.EN_ESPERA;
                if(!onlineMode){
                    taulerIntents.postDelayed(this::ferJugadaRobot, 500);
                }
            }
        } else {
            if (atacant == Jugador.RIVAL) {
                if(!onlineMode){
                    taulerIntents.postDelayed(this::ferJugadaRobot, 500);
                }
            }
        }

    }

    private void ferJugadaRobot() {
        if (estatJoc != EstatJoc.EN_ESPERA) return;

        Casella objectiu = null;

        if (direccioRival != null) {
            boolean valid = false;
            while (!valid) {
                Casella cand = new Casella(ultimImpacteRival.x + direccioRival[0], ultimImpacteRival.y + direccioRival[1]);
                if (esDinsGraella(cand) && !casellesRival.contains(cand)) {
                    objectiu = cand;
                    valid = true;
                } else {
                    direccioRival = new int[]{-direccioRival[0], -direccioRival[1]};
                    ultimImpacteRival = primerImpacteRival;

                    cand = new Casella(ultimImpacteRival.x + direccioRival[0], ultimImpacteRival.y + direccioRival[1]);
                    if (esDinsGraella(cand) && !casellesRival.contains(cand)) {
                        objectiu = cand;
                        valid = true;
                    } else {
                        direccioRival = null;
                        primerImpacteRival = null;
                        break;
                    }
                }
            }
        }

        if (objectiu == null && primerImpacteRival != null) {
            while (!direccionsRestants.isEmpty() && objectiu == null) {
                int[] dir = direccionsRestants.remove(0);
                Casella cand = new Casella(primerImpacteRival.x + dir[0], primerImpacteRival.y + dir[1]);
                if (esDinsGraella(cand) && !casellesRival.contains(cand)) {
                    objectiu = cand;
                }
            }
            if (objectiu == null) {
                primerImpacteRival = null;
            }
        }

        Random r = new Random();
        while (objectiu == null) {
            Casella cand = new Casella(r.nextInt(10), r.nextInt(10));
            if (!casellesRival.contains(cand)) {
                objectiu = cand;
            }
        }

        processarJugada(objectiu, Jugador.RIVAL);
    }

    private boolean esDinsGraella(Casella c) {
        return c.x >= 0 && c.x < 10 && c.y >= 0 && c.y < 10;
    }

    public void crearVaixells(Jugador jugador) {
        if (!vaixellsVius.containsKey(jugador)) {
            vaixellsVius.put(jugador, new HashMap<>());
            vaixells.put(jugador, new HashMap<>());
        }
        HashMap<Casella, Vaixell> caselles = vaixellsVius.get(jugador);
        HashMap<Casella, Vaixell> caselles2 = vaixells.get(jugador);
        int[] midaVaixells = {1, 1, 1, 1, 2, 2, 2, 3, 3, 4};
        int[] colors = {0xff706edd, 0xffdd866e, 0xffdd6ebe, 0xff6eddcf};
        Random rand = new Random();

        for (int mida : midaVaixells) {
            boolean colocat = false;
            while (!colocat) {
                int orientacioVal = rand.nextInt(2);
                Orientacio orientacio = (orientacioVal == 0) ? Orientacio.HORITZONTAL : Orientacio.VERTICAL;
                int maxX = (orientacio == Orientacio.HORITZONTAL) ? 10 - mida : 10;
                int maxY = (orientacio == Orientacio.VERTICAL) ? 10 - mida : 10;

                int x = rand.nextInt(maxX);
                int y = rand.nextInt(maxY);

                if (esPosicioValida(x, y, mida, orientacio, jugador)) {
                    Vaixell vaixell = new Vaixell();
                    vaixell.mida = mida;
                    vaixell.orientacio = orientacio;
                    vaixell.jugador = jugador;
                    vaixell.id = idVaixell++;
                    vaixell.tocat = 0;
                    vaixell.color = colors[mida - 1];

                    TreeSet<Casella> casellesMapping = new TreeSet<>(new Comparator<Casella>() {
                        @Override
                        public int compare(Casella o1, Casella o2) {
                            return o1.x == o2.x ? o1.y-o2.y : o1.x-o2.x;
                        }
                    });
                    for (int i = 0; i < mida; i++) {
                        int cx = x + (orientacio == Orientacio.HORITZONTAL ? i : 0);
                        int cy = y + (orientacio == Orientacio.VERTICAL ? i : 0);
                        casellesMapping.add(new Casella(cx, cy));
                        caselles.put(new Casella(cx, cy), vaixell);
                        caselles2.put(new Casella(cx, cy), vaixell);
                    }
                    if(!mappingVaixellCaselles.containsKey(jugador))mappingVaixellCaselles.put(jugador, new TreeMap<>(new Comparator<Vaixell>() {
                        @Override
                        public int compare(Vaixell o1, Vaixell o2) {
                            return o1.id - o2.id;
                        }
                    }));
                    mappingVaixellCaselles.get(jugador).put(vaixell, casellesMapping);
                    colocat = true;
                }
            }
        }
    }

    private boolean esPosicioValida(int x, int y, int mida, Orientacio orientacio, Jugador jugador) {
        if (orientacio == Orientacio.HORITZONTAL && x + mida - 1 > 9) return false;
        if (orientacio == Orientacio.VERTICAL && y + mida - 1 > 9) return false;

        Set<Casella> noves = new HashSet<>();
        HashMap<Casella, Vaixell> m = vaixellsVius.get(jugador);

        for (int i = 0; i < mida; i++) {
            int nx = orientacio == Orientacio.HORITZONTAL ? x + i : x;
            int ny = orientacio == Orientacio.VERTICAL ? y + i : y;
            Casella c = new Casella(nx, ny);

            if (m != null && m.containsKey(c)) return false;
            noves.add(c);
        }

        if (m == null) return true;

        for (Casella casella : m.keySet()) {
            for (Casella n : noves) {
                if (Math.max(Math.abs(n.x - casella.x), Math.abs(n.y - casella.y)) <= 1)
                    return false;
            }
        }
        return true;
    }

    private JSONObject construirJsonVaixells(int jugador) throws JSONException{
        JSONObject jsonVaixells = new JSONObject();
        JSONArray jsonCasellesVaixellsVius = new JSONArray();

        HashMap<Casella, Vaixell> mapa = vaixellsVius.get(Jugador.LOCAL);

        for(Casella c : mapa.keySet()){
            Vaixell v = mapa.get(c);

            JSONObject jsonCasella = new JSONObject();
            jsonCasella.put("i", c.y);
            jsonCasella.put("j", c.x);

            JSONObject jsonVaixell = new JSONObject();
            jsonVaixell.put("mida", v.mida);
            jsonVaixell.put("orientacio", v.orientacio == Orientacio.HORITZONTAL ? 0 : 1);
            jsonVaixell.put("color", v.color);
            jsonVaixell.put("id", v.id);

            JSONObject entrada = new JSONObject();
            entrada.put("casella", jsonCasella);
            entrada.put("vaixell", jsonVaixell);
            jsonCasellesVaixellsVius.put(entrada);
        }
        jsonVaixells.put("casellesVaixellsVius", jsonCasellesVaixellsVius);
        return jsonVaixells;
    }

    private void enviarCercarPartida(){
        try{
            JSONObject json = new JSONObject();
            json.put("tipus", "cercar_partida");
            gestorWebSocket.enviar(json);
        } catch(JSONException e){
            mostrarMissatge("Error enviat cercar_partida: " + e.getMessage());
        }
    }

    private void carregarVaixellsRival(JSONObject json){
        vaixellsVius.put(Jugador.RIVAL, new HashMap<>());
        vaixells.put(Jugador.RIVAL, new HashMap<>());

        try{
            JSONArray llista = json.getJSONArray("casellesVaixellsVius");

            for(int k = 0; k<llista.length(); k++){
                JSONObject entrada = llista.getJSONObject(k);

                JSONObject c = entrada.getJSONObject("casella");
                int i = c.getInt("i");
                int j = c.getInt("j");

                JSONObject v = entrada.getJSONObject("vaixell");
                int mida = v.getInt("mida");
                int orient = v.getInt("orientacio");
                int color = v.getInt("color");
                int id = v.getInt("id");

                Vaixell vaixell = new Vaixell();
                vaixell.mida = mida;
                vaixell.color = color;
                vaixell.id = id;
                vaixell.orientacio = (orient == 0 ? Orientacio.HORITZONTAL : Orientacio.VERTICAL);
                vaixell.jugador = Jugador.RIVAL;

                vaixellsVius.get(Jugador.RIVAL).put(new Casella(j,i), vaixell);
                vaixells.get(Jugador.RIVAL).put(new Casella(j,i), vaixell);
            }
        } catch(Exception e){
            mostrarMissatge("Error carregant vaixells rival: " + e.getMessage());
        }
    }

    private void gestionarPartidaTrobada(JSONObject json){
        boolean etToca = json.optBoolean("etToca", false);
        JSONObject rival = json.optJSONObject("rival");

        if(rival != null){
            JSONObject vaixellsRival = rival.optJSONObject("vaixells");
            if(vaixellsRival != null){
                carregarVaixellsRival(vaixellsRival);
            }
        }

        mostrarMissatge("Partida troabada!");

        torn = etToca ? Jugador.LOCAL : Jugador.RIVAL;
        estatJoc = etToca ? EstatJoc.JUGANT : EstatJoc.EN_ESPERA;

        repintarGraelles();
    }

    private void gestionarTirRebut(JSONObject json){
        try{
            int fila = json.getInt("fila");
            int col = json.getInt("columna");

            Casella c = new Casella(col,fila);

            processarJugada(c, Jugador.RIVAL);

            boolean encert = vaixellsTocats.get(Jugador.LOCAL).containsKey(c);
            boolean enfonsat = false;

            if(encert){
                Vaixell v = vaixellsTocats.get(Jugador.LOCAL).get(c);
                if(v.tocat == v.mida) enfonsat = true;
            }

            boolean acabat = vaixellsVius.get(Jugador.LOCAL).isEmpty();
            String resultat = encert ? (enfonsat ? "enfonsat" : "tocat") : "aigua";

            enviarResultatTir(c, resultat, acabat);

        } catch(JSONException e) {
            mostrarMissatge("Error tir rebut: " + e.getMessage());
        }
    }

    private void gestionarResultatTir(JSONObject json){
        try{
            int fila = json.getInt("fila");
            int col = json.getInt("columna");
            String resultat = json.getString("resultat");
            boolean acabat = json.getBoolean("acabat");

            Casella c = new Casella(col,fila);

            casellesLocal.add(c);
            historial.add(new Jugada(Jugador.LOCAL, c));

            if(resultat.equals("tocat") || resultat.equals("enfonsat")){
                Vaixell v = vaixellsVius.get(Jugador.RIVAL).get(c);
                vaixellsVius.get(Jugador.RIVAL).remove(c);
                vaixellsTocats.get(Jugador.RIVAL).put(c,v);
            }

            repintarGraelles();

            if(acabat){
                estatJoc = EstatJoc.ACABAT;
                mostrarMissatge("PARTIDA ACABADA: Has guanyat!");
                mostraFinalJoc(true);
                if (onlineMode) {
                    onlineMode = false;
                    gestorWebSocket.tancar();
                }

                return;
            }

            if(resultat.equals("aigua")){
                torn = Jugador.RIVAL;
                estatJoc = EstatJoc.EN_ESPERA;
            } else{
                torn = Jugador.LOCAL;
                estatJoc = EstatJoc.JUGANT;
            }
        } catch(JSONException e){
            mostrarMissatge("Error resultat tir: " + e.getMessage());
        }
    }

    private void enviarResultatTir(Casella c, String resultat, boolean acabat){
        try{
            JSONObject json = new JSONObject();
            json.put("tipus", "resultat_tir");
            json.put("fila", c.y);
            json.put("columna", c.x);
            json.put("resultat", resultat);
            json.put("acabat", acabat);
            gestorWebSocket.enviar(json);
        } catch(JSONException e){
            mostrarMissatge("Error enviat resultat tir: " + e.getMessage());
        }
    }

    private void gestionarAturaPartida(){

        mostrarMissatge("El rival ha sortir de la partida.");
        estatJoc = EstatJoc.ACABAT;
        onlineMode = false;
        gestorWebSocket.tancar();
    }

    private void mostrarMissatge(String txt) {
        missatges.append("\n" + txt);
        scroll();
    }

    public void connecta(){
        onlineMode = true;
        mostrarMissatge("Connectant al servidor...");
        gestorWebSocket.connectar("wss://hci.uib.es/ws");
        estatJoc = EstatJoc.EN_ESPERA;
        actualitzaBotons();
    }

    private void enviarRegistrar(String nomUsuari) {
        try{
            vaixellsVius.clear();
            vaixellsTocats.clear();
            casellesLocal.clear();
            casellesRival.clear();

            vaixellsTocats.put(Jugador.LOCAL, new HashMap<>());
            vaixellsTocats.put(Jugador.RIVAL, new HashMap<>());

            crearVaixells(Jugador.LOCAL);

            JSONObject json = new JSONObject();
            json.put("tipus", "registrar");
            json.put("nomUsuari", nomUsuari);
            json.put("vaixells", construirJsonVaixells(0));
            gestorWebSocket.enviar(json);
        } catch (JSONException e) {
            mostrarMissatge("Error␣enviant␣registrar:␣" + e.getMessage());
        }
    }

    private void enviarSortirPartida(){
        try{
            JSONObject json = new JSONObject();
            json.put("tipus", "sortir_partida");
            gestorWebSocket.enviar(json);
        } catch(Exception e){
            mostrarMissatge("Error enviant sortir_partida: " + e.getMessage());
        }
    }

    private void gestionarMissatge(JSONObject json) {
        try {
            String tipus = json.getString("tipus"); // llegir el tipus
            switch (tipus) { // processar el tipus
                case "connectat":
                    enviarRegistrar("Pirata");
                    break;
                case "registre_acceptat":
                    enviarCercarPartida();
                    break;
                case "esperant_rival":
                    mostrarMissatge("Esperant␣rival...");
                    break;
                case "partida_trobada":
                    gestionarPartidaTrobada(json);
                    break;
                case "tir_rebut":
                    gestionarTirRebut(json);
                    break;
                case "resultat_tir":
                    gestionarResultatTir(json);
                    break;
                case "rival_ha_sortit":
                case "rival_desconnectat":
                    gestionarAturaPartida();
                    break;
                case "error":
                    mostrarMissatge(json.optString("missatge"));
                    break;
                default:
                    mostrarMissatge("Missatge␣desconegut:␣" + json.toString());
                    break;
            }
        } catch (JSONException e) {
            mostrarMissatge("Error␣JSON:␣" + e.getMessage());
        }
    }
}