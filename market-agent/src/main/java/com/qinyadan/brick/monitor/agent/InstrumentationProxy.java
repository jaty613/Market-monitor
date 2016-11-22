package com.qinyadan.brick.monitor.agent;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.qinyadan.brick.monitor.agent.config.AgentConfig;
import com.qinyadan.brick.monitor.agent.service.ServiceFactory;
import com.qinyadan.brick.monitor.agent.utils.BootstrapLoader;
import com.qinyadan.brick.monitor.agent.utils.InstrumentationWrapper;
import com.qinyadan.brick.monitor.agent.utils.Streams;
import com.qinyadan.brick.monitor.agent.utils.Utils;

public class InstrumentationProxy extends InstrumentationWrapper {

	private final boolean bootstrapClassIntrumentationEnabled;

	protected InstrumentationProxy(Instrumentation instrumentation,
			boolean enableBootstrapClassInstrumentationDefault) {
		super(instrumentation);
		AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
		this.bootstrapClassIntrumentationEnabled = ((Boolean) config.getProperty(
				"enable_bootstrap_class_instrumentation", Boolean.valueOf(enableBootstrapClassInstrumentationDefault)))
						.booleanValue();
	}

	public static InstrumentationProxy getInstrumentationProxy(Instrumentation inst) {
		if (inst == null) {
			return null;
		}
		return new InstrumentationProxy(inst, true);
	}

	protected Instrumentation getInstrumentation() {
		return this.delegate;
	}

	public void redefineClasses(ClassDefinition... definitions)
			throws ClassNotFoundException, UnmodifiableClassException {
		if (isRedefineClassesSupported()) {
			super.redefineClasses(definitions);
		}
	}

	public Class<?>[] retransformUninstrumentedClasses(String... classNames)
			throws UnmodifiableClassException, ClassNotFoundException {
		if (!isRetransformClassesSupported()) {
			return new Class[0];
		}
		List<Class<?>> classList = new ArrayList<>(classNames.length);
		for (String className : classNames) {
			Class<?> clazz = Class.forName(className);
			classList.add(clazz);
		}
		Object classArray = (Class[]) classList.toArray(new Class[0]);
		if (!classList.isEmpty()) {
			retransformClasses((Class[]) classArray);
		}
		return (Class<?>[]) classArray;
	}

	public int getClassReaderFlags() {
		return 8;
	}

	public final boolean isBootstrapClassInstrumentationEnabled() {
		return this.bootstrapClassIntrumentationEnabled;
	}

	public boolean isAppendToClassLoaderSearchSupported() {
		return true;
	}

	public static void forceRedefinition(Instrumentation instrumentation, Class<?>... classes)
			throws ClassNotFoundException, UnmodifiableClassException {
		List<ClassDefinition> toRedefine = Lists.newArrayList();
		for (Class<?> clazz : classes) {
			String classResourceName = Utils.getClassResourceName(clazz);
			URL resource = clazz.getResource(classResourceName);
			if (resource == null) {
				resource = BootstrapLoader.get().findResource(classResourceName);
			}
			if (resource != null) {
				try {
					byte[] classfileBuffer = Streams.read(resource.openStream(), true);

					toRedefine.add(new ClassDefinition(clazz, classfileBuffer));
				} catch (Exception e) {
					// Agent.LOG.finer("Unable to redefine " + clazz.getName() +
					// " - " + e.toString());
				}
			} else {
				// Agent.LOG.finer("Unable to find resource to redefine " +
				// clazz.getName());
			}
		}
		if (!toRedefine.isEmpty()) {
			instrumentation.redefineClasses((ClassDefinition[]) toRedefine.toArray(new ClassDefinition[0]));
		}
	}
}
