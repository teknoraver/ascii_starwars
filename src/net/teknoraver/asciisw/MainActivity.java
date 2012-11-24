package net.teknoraver.asciisw;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class MainActivity extends Activity implements Runnable, OnClickListener {
	static final int FPS = 15;

	private AssetManager am;		
	private XmlPullParser in;
	private Handler handler;
	private Screen screen;
	private MediaPlayer mp;
	private AssetFileDescriptor lastAfd;
	private Thread projector;
	private Frame frame;
	private ImageButton playpause;
	private int curChapter;
	private int skipTo;
	private int chapters;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		projector = new Thread(this);
		am = getAssets();
		handler = new Handler();
		mp = new MediaPlayer();

		screen = (Screen)findViewById(R.id.screen);

		((ImageButton)findViewById(R.id.play)).setOnClickListener(this);
		((ImageButton)findViewById(R.id.next)).setOnClickListener(this);
		((ImageButton)findViewById(R.id.stop)).setOnClickListener(this);
		((ImageButton)findViewById(R.id.chapter)).setOnClickListener(this);

		playpause = (ImageButton)findViewById(R.id.play);
		playpause.setOnClickListener(this);

		init();
	}

	@Override
	protected void onStart() {
		super.onStart();

		if(projector != null)
			projector.start();
		else
			play();
	}

	@Override
	protected void onPause() {
		super.onPause();

		projector = null;
		mp.stop();
	}

	private Frame getFrame() throws IOException, XmlPullParserException {
		in.nextTag();
		if(in.getEventType() == XmlPullParser.END_TAG) {
			in.next();
			if(in.getEventType() == XmlPullParser.END_DOCUMENT)
				return null;
		}

		in.require(XmlPullParser.START_TAG, null, "frame");
			
		int delay = Integer.parseInt(in.getAttributeValue(null, "duration"));

		String chaps = in.getAttributeValue(null, "chapter");
		if(chaps != null) {
			curChapter = Integer.parseInt(chaps);
			if(skipTo >= 0 && curChapter >= skipTo)
				skipTo = -1;
//System.out.println("Chapter: " + curChapter);
		}

		String sound = in.getAttributeValue(null, "sound");
		if(skipTo < 0 && sound != null) {
			if(lastAfd != null) {
				mp.stop();
				mp.reset();
				lastAfd.close();
				lastAfd = null;
			}
			try {
				lastAfd = am.openFd("sounds/" + sound + ".ogg");
				mp.setDataSource(lastAfd.getFileDescriptor(),
						lastAfd.getStartOffset(),
						lastAfd.getLength());
				mp.prepare();
				mp.start();
			} catch(FileNotFoundException fnfe) { }
		}

//		System.err.println("delay: " + delay);

		if(delay < 0)
			return null;

		String framestring = in.nextText();
		in.require(XmlPullParser.END_TAG, null, null);

		// every frame has a leading ".\n" to avoid the XML parser to trim the leading space
		String rows[] = framestring.substring(2).split("\n");		

		frame = new Frame(delay, rows);
		if(chaps != null)
			frame.comment = "Chapter " + curChapter;
		return frame;
	}

	@Override
	public void run() {
		try {
			while(projector != null && (frame = getFrame()) != null) {
				if(skipTo >= 0)
					if(curChapter < skipTo)
						continue;
				handler.post(drawer);
				Thread.sleep(frame.duration * 1000 / FPS);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		frame = null;
	}

	private void init() {
		try {
			in = XmlPullParserFactory.newInstance().newPullParser();
			in.setInput(new GZIPInputStream(am.open("sw.xml.jpg")), null);
	
			// Start
			in.require(XmlPullParser.START_DOCUMENT, null, null);
	
			in.nextTag();
			in.require(XmlPullParser.START_TAG, null, "frameset");
			chapters = Integer.parseInt(in.getAttributeValue(null, "chapters"));
		} catch (Exception e) {
			e.printStackTrace();
			finish();
		}
	}

	private void stop() {
		projector = null;
		curChapter = 0;
		playpause.setImageResource(R.drawable.play);
		mp.reset();
		if(lastAfd != null)
			try { lastAfd.close(); } catch (IOException e) { }
		lastAfd = null;
		projector = null;
		in = null;
		screen.clear();
		while(frame != null)
			try { Thread.sleep(100); } catch (InterruptedException e) { }
		System.gc();
	}

	private void play() {
		if(in == null)
			init();
		playpause.setImageResource(R.drawable.pause);
		mp.start();
		projector = new Thread(this);
		projector.start();
	}

	private final Runnable drawer = new Runnable() {
		@Override
		public void run() {
			screen.setText(frame);
		}
	};

	@Override
	public void onClick(View view) {
		switch(view.getId()) {
		case R.id.chapter:
			final String items[] = new String[chapters];
			for(int i = 0; i < chapters; i++)
				items[i] = String.valueOf(i + 1);

			new AlertDialog.Builder(this)
				.setTitle("Select chapter")
				.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						stop();
						skipTo = item + 1;
						play();
					}
				})
				.create().show();
			break;
		case R.id.next:
			skipTo = curChapter + 1;
			mp.reset();
			break;
		case R.id.play:
			if(projector == null) {
				play();
			} else {
				playpause.setImageResource(R.drawable.play);
				mp.pause();
				projector = null;
			}
			break;
		case R.id.stop:
			stop();
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu m) {
                getMenuInflater().inflate(R.menu.menu, m);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		new AlertDialog.Builder(this)
			.setTitle(R.string.app_name)
			.setMessage(R.string.desc)
			.setIcon(android.R.drawable.ic_dialog_info)
			.show();
		return true;
	}
}
