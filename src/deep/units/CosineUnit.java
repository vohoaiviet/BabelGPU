package deep.units;

import gpu.*;

public class CosineUnit extends ElementComputeUnit
{
	public CosineUnit(String name)
	{
		super(name);
	}

	@Override
	public void forward()
	{
		Thrust.cos(input.data, output.data);
	}

	@Override
	public void backward_element()
	{
		Thrust.sin(input.data, input.gradient);
		input.gradient.linear(-1, 0);
	}

}