package at.gitterleem.application.core.scene;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Chunk {
	private int xStart;
	private int xEnd;
	private int yStart;
	private int yEnd;
}
