package deep.units;

import deep.DeepException;

/**
 * For forward only calculations.
 * Isn't actually a neural network. Doesn't support back-prop
 */
public class ForwardOnlyTUnit extends TerminalUnit
{
	private static final long serialVersionUID = 1L;

	public ForwardOnlyTUnit(String name, InletUnit inlet)
	{
		super(name, inlet);
	}
	
	@Override
	public void setup()
	{
		super.setup();
		this.output = this.input;
	}
	
	@Override
	public final void forward() { }

	@Override
	protected float forward_terminal(boolean doesCalcLoss) { return 0; }

	@Override
	public final void backward()
	{
		throw new DeepException(
				"Forward-only terminal doesn't support back-prop. "
				+ "It's meant for pure calculation only.");
	}

}
