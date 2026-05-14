package com.example.hundirlaflota;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
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

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    //Estat del joc
    enum EstatJoc{ATURADA, JUGANT}
    EstatJoc estatJoc = EstatJoc.ATURADA;


    //Elements de la interficie
    TextView titol;
    TextView DarreraJugada1;
    TextView DarreraJugada2;
    TextView missatges;

    SurfaceView taulerIntents;
    SurfaceView taulerVaixells;

    ImageButton atura;
    ImageButton newGame;
    ImageButton online;
    ImageButton tip;

    Set<View> conjuntPistes = new HashSet<>();

    ConstraintLayout zonaPistes;
    ConstraintLayout caixaPistes;
    TextView titolMeus, titolRival, textPistesMeus, textPistesRival;
    ImageButton botoTancarPistes;


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

        //Assignació de ID's
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

        missatges.setMovementMethod(new ScrollingMovementMethod());
        textPistesMeus.setMovementMethod(new ScrollingMovementMethod());
        textPistesRival.setMovementMethod(new ScrollingMovementMethod());

        //Dibuix fons dels dos taulers
        taulerVaixells.post(() -> pintar(taulerVaixells,10,10));
        taulerIntents.post(() -> pintar(taulerIntents,10,10));

        conjuntPistes.add(zonaPistes);
        conjuntPistes.add(caixaPistes);
        conjuntPistes.add(titolMeus);
        conjuntPistes.add(titolRival);
        conjuntPistes.add(textPistesMeus);
        conjuntPistes.add(textPistesRival);
        conjuntPistes.add(botoTancarPistes);

        // Inicialment, ocultar la zona de pistes
        mostraPistes(false);

        //Accions dels botons
        newGame.setOnClickListener(v -> {
            estatJoc = EstatJoc.JUGANT;
            actualitzaBotons();
            missatges.append("\nNou joc iniciat.");
        });

        online.setOnClickListener(v -> {
            estatJoc = EstatJoc.JUGANT;
            actualitzaBotons();
            missatges.append("\nConnectant...");
        });

        atura.setOnClickListener(v -> {
            estatJoc = EstatJoc.ATURADA;
            actualitzaBotons();
            missatges.append("\nPartida aturada.");
        });

        tip.setOnClickListener(v -> {
            textPistesMeus.setText("Vaixell de 1 (1): (2,9)\nVaixell de 1(2): (5,8)\n...");
            textPistesRival.setText("Vaixell de 1 (1): (9,0)\nVaixell de 1 (2): (0,9)\n...");
            mostraPistes(true);
        });

        botoTancarPistes.setOnClickListener(v -> mostraPistes(false));

        // Estat inicial
        actualitzaBotons();
    }
    //Control de l'estat dels botons
    private void actualitzaBotons() {
        if (estatJoc == EstatJoc.ATURADA) {
            newGame.setEnabled(true);
            online.setEnabled(true);

            atura.setEnabled(false);
            tip.setEnabled(false);
        } else { // JUGANT
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
    }
    public void pintar(SurfaceView tauler, int files, int columnes){
        if(tauler.getHolder().getSurface().isValid()){
            Canvas canvas = tauler.getHolder().lockCanvas();
            pintar(tauler, files, columnes, canvas);
            tauler.getHolder().unlockCanvasAndPost(canvas);
        }
    }

    private void drawSingleGrid(Casella c, SurfaceView tauler){
        if(tauler.getHolder().getSurface().isValid()){
            Canvas canvas = tauler.getHolder().lockCanvas();
            /*
            Hardcoded amount of rows and columns.
            Maybe change that?
             */
            pintar(tauler, 10, 10, canvas);
            float x = tauler.getWidth()/(float)10;
            float y = tauler.getHeight()/(float)10;
            Paint p = new Paint();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(5);
            p.setColor(Color.RED);
            canvas.drawRoundRect(c.x*x,c.y*y,c.x*x + x,c.y*y + y,25,25,p);
            tauler.getHolder().unlockCanvasAndPost(canvas);
        }
    }

    public void drawGrids(Casella c){
        drawSingleGrid(c, taulerIntents);
    }

    private Casella getCasella(float x, float y){
        int posX = (int)Math.floor(((x - taulerIntents.getX())/taulerIntents.getWidth())*10);
        int posY = (int)Math.floor(((y - taulerIntents.getY())/taulerIntents.getHeight())*10);
        missatges.append(String.format("\nCASELLA: (%s,%s)", posX, posY));
        DarreraJugada2.setText(String.format("Seleccionada la casella (%s, %s)", posX, posY));
        return new Casella(posX, posY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(
                event.getX()>=taulerIntents.getX() &&
                event.getX()<=taulerIntents.getX()+taulerIntents.getWidth() &&
                event.getY()>=taulerIntents.getY() &&
                event.getY()<=taulerIntents.getY()+taulerIntents.getHeight()
        ){
            if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                drawGrids(getCasella(event.getX(), event.getY()));
            }
        }
        return true;
    }

    //Mostrar o amagar totes les vistes de pistes
    private void mostraPistes(boolean mostrar) {
        for (View v : conjuntPistes) {
            v.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        }
    }
}