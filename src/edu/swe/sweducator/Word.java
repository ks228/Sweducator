package edu.swe.sweducator;

public class Word {
	private int id;
	private String word;
	private String definition;
	private String soundfile;
	
	public Word(int id, String word, String definition, String soundfile) {
		super();
		this.id = id;
		this.word = word;
		this.definition = definition;
		this.soundfile = soundfile;
	}
	
	public Word(int id, String word, String definition) {
		this(id, word, definition, "");
	}

	public Word(String word, String definition) {
		this(0, word, definition, "");
	}

	public int getId() {
		return id;
	}

	public String getWord() {
		return word;
	}

	public String getDefinition() {
		return definition;
	}

	public String getSoundfile() {
		return soundfile;
	}
	
	@Override
	public String toString() {
		return "id=" + id + " word=" + word + "def=" + 
				definition + "soundfile=" + soundfile;
	}
	
}
