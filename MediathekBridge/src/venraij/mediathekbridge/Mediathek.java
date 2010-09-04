package venraij.mediathekbridge;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

import javax.swing.JComponent;

import mediathek.Konstanten;
import mediathek.daten.Daten;
import mediathek.daten.DatenFilm;
import mediathek.daten.ListeFilme;
import mediathek.io.FilmListener;
import mediathek.io.MediathekReader;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.WebVideoStream;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.external.AdditionalFolderAtRoot;

public class Mediathek implements AdditionalFolderAtRoot {
	private static String[] tttopics = new String[] { "Abenteuer Wissen",
			"Abenteuer Forschung", "auslandsjournal", "heute-show", "History",
			"neues(3sat)", "TerraX", "ZDF.reporer", "ZDF.reportage",
			"ZDF.umwelt", "alles wissen", "ARD-Brennpunkt", "c't-Magazin",
			"Tagesschau", "Tagesthemen", "hitec", "nano" };

	private DLNAResource root;
	private Daten mediathekData;

	public Mediathek() {
		root = new VirtualFolder("Mediathek", "");
		mediathekData = new Daten(".", true, null, true /* noGui */);
		initListener();
		new Thread("MediathekSyncronizer") {
			@Override
			public void run() {
				while (true) {
					mediathekData.listeFilme.fillListe();
					try {
						sleep(1000 * 600);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	private void initListener() {
		FilmListener beobachterLadenFilme = new FilmListener() {
			@Override
			public void addMax(int max) {
			}

			@Override
			public void fertig(boolean stop) {
				updateRoot();
			}

			@Override
			public void progress(String text) {
			}

			@Override
			public void start(int max) {
			}
		};
		Iterator<MediathekReader> it = mediathekData.mediathekListe.iterator();
		while (it.hasNext()) {
			it.next().addAdListener(beobachterLadenFilme);
		}
	}

	@Override
	public DLNAResource getChild() {
		updateRoot();
		return root;
	}

	@Override
	public JComponent config() {
		return null;
	}

	@Override
	public String name() {
		return "Mediathek";
	}

	@Override
	public void shutdown() {
	}

	private boolean intereestedInTopic(String topic) {
		for (int i = 0; i < tttopics.length; i++) {
			if (tttopics[i].equalsIgnoreCase(topic))
				return true;
		}
		return false;
	}

	///videofeed.Web,Youtube,PS3=http://www.youtube.com/ut_rss?type=username&arg
	// =matt9339
	private void updateRoot() {

		synchronized (mediathekData) {
			ListeFilme filme = (ListeFilme) mediathekData.listeFilme;
			for (int i = 0; i < filme.size(); i++) {
				DatenFilm film = filme.get(i);
				String topic = film.arr[Konstanten.FILM_THEMA_NR];
				if (!intereestedInTopic(topic))
					continue;
				DLNAResource node = root;
				node = goDownToTopic(node, topic);

				String titel = film.arr[Konstanten.FILM_TITEL_NR];
				String url = film.arr[Konstanten.FILM_URL_HD_NR];
				if (url.trim().length() == 0)
					url = film.arr[Konstanten.FILM_URL_NR];

				DLNAResource titleNode = node.searchByName(titel);
				if (titleNode == null) {
					// WebVideoStream(String fluxName, String URL, String
					// thumbURL)
					node.addChild(new WebVideoStream(titel, transformUrl(url),
							""));
				}

			}
		}
	}

	private DLNAResource goDownToTopic(DLNAResource parent, String topic) {
		DLNAResource node = parent.searchByName(topic);
		if (node != null)
			return node;
		node = new VirtualFolder(topic, "");
		parent.addChild(node);

		return node;
	}

	private String transformUrl(String orig) {
		if (orig.endsWith("asx")) {
			URL url;
			try {
				// "http://wstreaming.zdf.de/zdf/300/100721_corexit_awi.asx"
				url = new URL(orig);
				InputStream in = url.openStream();
				StringBuilder sb = new StringBuilder();
				int i;
				while ((i = in.read()) > 0) {
					sb.append((char) i);
				}
				in.close();
				String s = sb.toString();
				i = s.indexOf("href=\"");
				if (i >= 0) {
					i += 6;
					int j = s.indexOf("\"", i);
					return s.substring(i, j);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return orig;

	}

}
