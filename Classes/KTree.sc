KTree {
	var masterGroup;

	*new { |tree|
		^super.new.resetTree.build(tree);
	}

	build { |tree|
		var s				= Server.default;
		var buses			= Dictionary.new,
			groups			= Dictionary.new,
			synths			= Dictionary.new;
		var groupCounter	= 0,
			busCounter		= 0,
			synthCounter	= 0;

		var recurse = { |list, parent, parentBus|
			var previousBus = parentBus;
			var previousGroup = nil;

			list.do { |child|
				if (child.type == \group) {
					var group = Group.tail(parent);
					var bus = recurse.value(child.children, group, previousBus);

					previousGroup = group;
					previousBus = bus;

					if (child.name == nil) {
						buses.put(busCounter, bus);
						groups.put(groupCounter, group);
						busCounter = busCounter + 1;
						groupCounter = groupCounter + 1;
					} {
						buses.put(child.name, bus);
						groups.put(child.name, group);
					};
				} {
					var synth = Synth.tail(parent, child.type),
						outBus = if (child.outSibling == true)
									{previousBus} {Bus.audio(s, 2)};

					synth.set(\in, previousBus);
					synth.set(\out, outBus);
					synth.set(*child.params);

					previousBus = if (child.inSibling == true) {previousBus} {outBus};

					if (child.name == nil) {
						synths.put(synthCounter, synth);
						synthCounter = synthCounter + 1;
					} {
						synths.put(child.name, synth);
						buses.put(child.name, outBus);
					};
				};
			};
			// return
			previousBus;

		};
		recurse.value(tree, masterGroup);

		^(
			masterGroup: masterGroup,
			buses: buses,
			groups: groups,
			synths: synths,
		);
	}

	resetTree {
		if (masterGroup.isKindOf(Group.class)) {
			masterGroup.freeAll;
			Group.replace(masterGroup);
		} {
			masterGroup = Group.new;
		}
	}
}