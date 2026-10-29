package com.flansmodultimate.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Checks a mixin configuration against the bytecode on the test classpath, without applying the mixins or loading any
 * class they name. Minecraft 1.21.1 runs Mojang names in development and production, so the names a mixin gives must
 * match the installed Minecraft and Custom NPCs classes exactly: a missing target only fails when the game loads it.
 */
public final class MixinTargetCheck
{
    private static final String INJECTION = "Lorg/spongepowered/asm/mixin/injection/";
    private static final Set<String> INJECTORS = Set.of(INJECTION + "Inject;", INJECTION + "Redirect;", INJECTION + "ModifyVariable;", INJECTION + "ModifyArg;", INJECTION + "ModifyArgs;",
        INJECTION + "ModifyConstant;");
    private static final String MODIFY_CONSTANT = INJECTION + "ModifyConstant;";
    private static final String INJECT = INJECTION + "Inject;";
    private static final String REDIRECT = INJECTION + "Redirect;";
    private static final String CALLBACK = "org/spongepowered/asm/mixin/injection/callback/CallbackInfo";
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String ACCESSOR = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER = "Lorg/spongepowered/asm/mixin/gen/Invoker;";

    private final Map<String, Optional<ClassNode>> classes = new HashMap<>();
    private final List<String> problems = new ArrayList<>();
    private int defaultRequire = 1;
    private int checked;

    private MixinTargetCheck()
    {}

    /** What a check found: the members the bytecode lacks, and how many injectors, shadows and accessors it looked up. */
    public record Result(List<String> problems, int checked)
    {}

    /** Looks up every target, injection point, shadow and accessor of the configuration in the bytecode. */
    public static Result check(String config) throws IOException
    {
        MixinTargetCheck check = new MixinTargetCheck();
        JsonObject json;
        try (InputStream input = MixinTargetCheck.class.getClassLoader().getResourceAsStream(config))
        {
            if (input == null)
                throw new IOException("Missing mixin configuration " + config);
            json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        if (json.has("injectors") && json.getAsJsonObject("injectors").has("defaultRequire"))
            check.defaultRequire = json.getAsJsonObject("injectors").get("defaultRequire").getAsInt();
        String packagePath = json.get("package").getAsString().replace('.', '/');
        for (String side : List.of("mixins", "client", "server"))
        {
            if (!json.has(side))
                continue;
            for (JsonElement name : json.getAsJsonArray(side))
                check.checkMixin(packagePath + "/" + name.getAsString().replace('.', '/'));
        }
        return new Result(check.problems, check.checked);
    }

    private void checkMixin(String mixinName)
    {
        Optional<ClassNode> mixin = read(mixinName);
        if (mixin.isEmpty())
        {
            problems.add(mixinName + ": the configured mixin class is missing");
            return;
        }
        String simpleName = mixinName.substring(mixinName.lastIndexOf('/') + 1);
        AnnotationNode annotation = annotation(mixin.get().visibleAnnotations, mixin.get().invisibleAnnotations, MIXIN);
        if (annotation == null)
        {
            problems.add(simpleName + ": has no @Mixin annotation");
            return;
        }
        List<String> targets = new ArrayList<>();
        for (Object type : list(value(annotation, "value")))
            targets.add(((Type) type).getInternalName());
        for (Object target : list(value(annotation, "targets")))
            targets.add(((String) target).replace('.', '/'));
        for (String targetName : targets)
        {
            Optional<ClassNode> target = read(targetName);
            if (target.isEmpty())
                problems.add(simpleName + ": target class " + targetName + " is missing");
            else
                checkMembers(simpleName, mixin.get(), target.get());
        }
    }

    private void checkMembers(String mixinName, ClassNode mixin, ClassNode target)
    {
        String where = mixinName + " -> " + target.name;
        for (FieldNode field : mixin.fields)
        {
            if (annotation(field.visibleAnnotations, field.invisibleAnnotations, SHADOW) != null)
            {
                checked++;
                if (!hasField(target, field.name, field.desc))
                    problems.add(where + ": shadowed field " + field.name + " " + field.desc + " is missing");
            }
        }
        for (MethodNode method : mixin.methods)
        {
            if (annotation(method.visibleAnnotations, method.invisibleAnnotations, SHADOW) instanceof AnnotationNode shadow)
            {
                String prefix = Objects.requireNonNullElse((String) value(shadow, "prefix"), "shadow$");
                String name = method.name.startsWith(prefix) ? method.name.substring(prefix.length()) : method.name;
                checked++;
                if (!hasMethod(target, name, method.desc))
                    problems.add(where + ": shadowed method " + name + method.desc + " is missing");
            }
            if (annotation(method.visibleAnnotations, method.invisibleAnnotations, ACCESSOR) instanceof AnnotationNode accessor)
                checkAccessor(where, target, method, accessor);
            if (annotation(method.visibleAnnotations, method.invisibleAnnotations, INVOKER) instanceof AnnotationNode invoker)
                checkInvoker(where, target, method, invoker);
            for (AnnotationNode injector : annotations(method.visibleAnnotations, method.invisibleAnnotations))
            {
                if (INJECTORS.contains(injector.desc))
                    checkInjector(where + " (" + method.name + ")", target, injector, method);
            }
        }
    }

    private void checkAccessor(String where, ClassNode target, MethodNode method, AnnotationNode accessor)
    {
        checked++;
        String name = (String) value(accessor, "value");
        if (name == null || name.isEmpty())
            name = decapitalize(method.name.replaceFirst("^(get|set|is)", ""));
        String fieldName = name;
        if (target.fields.stream().noneMatch(field -> field.name.equals(fieldName)))
            problems.add(where + ": accessed field " + fieldName + " is missing");
    }

    private void checkInvoker(String where, ClassNode target, MethodNode method, AnnotationNode invoker)
    {
        checked++;
        String name = (String) value(invoker, "value");
        if (name == null || name.isEmpty())
            name = decapitalize(method.name.replaceFirst("^(call|invoke)", ""));
        String methodName = name;
        if (target.methods.stream().noneMatch(candidate -> candidate.name.equals(methodName) && candidate.desc.equals(method.desc)))
            problems.add(where + ": invoked method " + methodName + method.desc + " is missing");
    }

    private void checkInjector(String where, ClassNode target, AnnotationNode injector, MethodNode handler)
    {
        Object requireValue = value(injector, "require");
        int require = requireValue instanceof Integer explicit ? explicit : defaultRequire;
        if (require <= 0)
            return;
        checked++;
        List<String> selectors = new ArrayList<>();
        for (Object selector : list(value(injector, "method")))
            selectors.add((String) selector);
        List<AnnotationNode> points = new ArrayList<>();
        for (Object point : list(value(injector, "at")))
            points.add((AnnotationNode) point);
        List<AnnotationNode> constants = new ArrayList<>();
        if (injector.desc.equals(MODIFY_CONSTANT))
        {
            for (Object constant : list(value(injector, "constant")))
                constants.add((AnnotationNode) constant);
        }
        int found = 0;
        boolean selected = false;
        for (MethodNode method : target.methods)
        {
            if (selectors.stream().noneMatch(selector -> selects(selector, method)))
                continue;
            selected = true;
            if (injector.desc.equals(MODIFY_CONSTANT))
                found += (int) constants.stream().filter(constant -> hasConstant(method, constant)).count();
            else
                found += (int) points.stream().filter(point -> matches(point, method)).count();
            checkHandler(where, injector, handler, method, points);
        }
        if (!selected)
            problems.add(where + ": target method " + selectors + " is missing");
        else if (found < require)
            problems.add(where + ": " + found + " of " + require + " required injection points found in " + selectors + " for " + describe(points, constants));
    }

    /** Mixin rejects a handler whose parameters do not fit the target method or the redirected call. */
    private void checkHandler(String where, AnnotationNode injector, MethodNode handler, MethodNode method, List<AnnotationNode> points)
    {
        Type[] targetArguments = Type.getArgumentTypes(method.desc);
        Type[] handlerArguments = Type.getArgumentTypes(handler.desc);
        if (injector.desc.equals(INJECT))
        {
            int callback = -1;
            for (int index = 0; index < handlerArguments.length && callback < 0; index++)
            {
                if (handlerArguments[index].getInternalName().startsWith(CALLBACK))
                    callback = index;
            }
            if (callback < 0 || callback > 0 && (callback != targetArguments.length || !startsWith(handlerArguments, targetArguments)))
                problems.add(where + ": parameters " + handler.desc + " do not fit " + method.name + method.desc);
            return;
        }
        if (!injector.desc.equals(REDIRECT))
            return;
        for (AnnotationNode point : points)
        {
            if (!"INVOKE".equals(value(point, "value")))
                continue;
            for (AbstractInsnNode instruction : method.instructions)
            {
                if (instruction instanceof MethodInsnNode call && countCalls(singleCall(method, call), (String) value(point, "target")) > 0)
                {
                    List<Type> expected = new ArrayList<>();
                    if (call.getOpcode() != org.objectweb.asm.Opcodes.INVOKESTATIC)
                        expected.add(Type.getObjectType(call.owner));
                    expected.addAll(List.of(Type.getArgumentTypes(call.desc)));
                    Type[] prefix = expected.toArray(Type[]::new);
                    boolean fits = startsWith(handlerArguments, prefix) && (handlerArguments.length == prefix.length || handlerArguments.length == prefix.length + targetArguments.length
                        && startsWith(Arrays.copyOfRange(handlerArguments, prefix.length, handlerArguments.length), targetArguments));
                    if (!fits || !Type.getReturnType(handler.desc).equals(Type.getReturnType(call.desc)))
                        problems.add(where + ": " + handler.desc + " does not fit the redirected " + call.owner + "." + call.name + call.desc + " in " + method.name);
                    return;
                }
            }
        }
    }

    /** A method holding only the call, so the target matcher can test that one instruction. */
    private static MethodNode singleCall(MethodNode method, MethodInsnNode call)
    {
        MethodNode single = new MethodNode(method.access, method.name, method.desc, null, null);
        single.instructions.add(new MethodInsnNode(call.getOpcode(), call.owner, call.name, call.desc, call.itf));
        return single;
    }

    private static boolean startsWith(Type[] arguments, Type[] prefix)
    {
        if (arguments.length < prefix.length)
            return false;
        for (int index = 0; index < prefix.length; index++)
        {
            if (!arguments[index].equals(prefix[index]))
                return false;
        }
        return true;
    }

    private static boolean selects(String selector, MethodNode method)
    {
        String name = selector;
        int ownerEnd = name.indexOf(';');
        if (name.startsWith("L") && ownerEnd > 0 && (name.indexOf('(') < 0 || ownerEnd < name.indexOf('(')))
            name = name.substring(ownerEnd + 1);
        int arguments = name.indexOf('(');
        if (arguments < 0)
        {
            // A trailing * selects every overload of the name.
            String plain = name.endsWith("*") ? name.substring(0, name.length() - 1) : name;
            return plain.isEmpty() || method.name.equals(plain);
        }
        return method.name.equals(name.substring(0, arguments)) && method.desc.equals(name.substring(arguments));
    }

    /** Whether the injection point occurs in the method often enough for its ordinal. */
    private static boolean matches(AnnotationNode point, MethodNode method)
    {
        String kind = (String) value(point, "value");
        String target = (String) value(point, "target");
        Object ordinalValue = value(point, "ordinal");
        int ordinal = ordinalValue instanceof Integer explicit ? explicit : -1;
        Object opcodeValue = value(point, "opcode");
        int opcode = opcodeValue instanceof Integer explicit ? explicit : -1;
        return switch (kind)
        {
            case "INVOKE", "INVOKE_ASSIGN", "INVOKE_STRING" -> occursOften(countCalls(method, target), ordinal);
            case "FIELD" -> occursOften(countFields(method, target, opcode), ordinal);
            case "NEW" -> matchesNew(method, target, ordinal);
            default -> occursOften(1, ordinal); // HEAD, RETURN, TAIL, LOAD, STORE and constants are found in any method with a body.
        };
    }

    private static boolean occursOften(int occurrences, int ordinal)
    {
        return ordinal < 0 ? occurrences > 0 : occurrences > ordinal;
    }

    private static int countCalls(MethodNode method, String target)
    {
        String owner = null;
        String member = target;
        if (member.startsWith("L") && member.indexOf(';') > 0 && member.indexOf(';') < member.indexOf('('))
        {
            owner = member.substring(1, member.indexOf(';'));
            member = member.substring(member.indexOf(';') + 1);
        }
        int arguments = member.indexOf('(');
        String name = arguments < 0 ? member : member.substring(0, arguments);
        String descriptor = arguments < 0 ? null : member.substring(arguments);
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions)
        {
            if (instruction instanceof MethodInsnNode call && call.name.equals(name) && (owner == null || call.owner.equals(owner)) && (descriptor == null || call.desc.equals(descriptor)))
                count++;
        }
        return count;
    }

    private static int countFields(MethodNode method, String target, int opcode)
    {
        String owner = target.substring(1, target.indexOf(';'));
        String member = target.substring(target.indexOf(';') + 1);
        int separator = member.indexOf(':');
        String name = separator < 0 ? member : member.substring(0, separator);
        String descriptor = separator < 0 ? null : member.substring(separator + 1);
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions)
        {
            if (instruction instanceof FieldInsnNode field && field.owner.equals(owner) && field.name.equals(name) && (descriptor == null || field.desc.equals(descriptor))
                && (opcode < 0 || field.getOpcode() == opcode))
                count++;
        }
        return count;
    }

    /**
     * Mirrors Mixin's {@code BeforeNew}: the ordinal counts every {@code NEW} of the type, whatever constructor follows it,
     * and a descriptor then only needs a matching constructor call somewhere after the selected {@code NEW}.
     */
    private static boolean matchesNew(MethodNode method, String target, int ordinal)
    {
        String type = target;
        String constructor = null;
        if (target.startsWith("("))
        {
            constructor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getArgumentTypes(target));
            type = Type.getReturnType(target).getInternalName();
        }
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions)
        {
            if (!(instruction instanceof TypeInsnNode created) || created.getOpcode() != Opcodes.NEW || !created.desc.equals(type))
                continue;
            if ((ordinal < 0 || ordinal == index) && (constructor == null || hasConstructorAfter(created, type, constructor)))
                return true;
            index++;
        }
        return false;
    }

    private static boolean hasConstructorAfter(AbstractInsnNode created, String type, String constructor)
    {
        for (AbstractInsnNode instruction = created.getNext(); instruction != null; instruction = instruction.getNext())
        {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESPECIAL && call.name.equals("<init>") && call.owner.equals(type) && call.desc.equals(constructor))
                return true;
        }
        return false;
    }

    private static boolean hasConstant(MethodNode method, AnnotationNode constant)
    {
        Object expected = null;
        for (String key : List.of("intValue", "floatValue", "longValue", "doubleValue", "stringValue"))
        {
            if (value(constant, key) != null)
                expected = value(constant, key);
        }
        if (expected == null)
            return true;
        for (AbstractInsnNode instruction : method.instructions)
        {
            Object actual = switch (instruction)
            {
                case LdcInsnNode ldc -> ldc.cst;
                case IntInsnNode push -> push.operand;
                case InsnNode insn when insn.getOpcode() >= 2 && insn.getOpcode() <= 8 -> insn.getOpcode() - 3;
                case InsnNode insn when insn.getOpcode() >= 11 && insn.getOpcode() <= 13 -> (float) (insn.getOpcode() - 11);
                case InsnNode insn when insn.getOpcode() == 14 || insn.getOpcode() == 15 -> (double) (insn.getOpcode() - 14);
                default -> null;
            };
            if (expected.equals(actual))
                return true;
        }
        return false;
    }

    private boolean hasField(ClassNode type, String name, String descriptor)
    {
        for (ClassNode current = type; current != null; current = current.superName == null ? null : read(current.superName).orElse(null))
        {
            if (current.fields.stream().anyMatch(field -> field.name.equals(name) && field.desc.equals(descriptor)))
                return true;
        }
        return false;
    }

    private boolean hasMethod(ClassNode type, String name, String descriptor)
    {
        for (ClassNode current = type; current != null; current = current.superName == null ? null : read(current.superName).orElse(null))
        {
            if (current.methods.stream().anyMatch(method -> method.name.equals(name) && method.desc.equals(descriptor)))
                return true;
        }
        return false;
    }

    private Optional<ClassNode> read(String internalName)
    {
        return classes.computeIfAbsent(internalName, name ->
        {
            try (InputStream input = MixinTargetCheck.class.getClassLoader().getResourceAsStream(name + ".class"))
            {
                if (input == null)
                    return Optional.empty();
                ClassNode node = new ClassNode();
                new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                return Optional.of(node);
            }
            catch (IOException exception)
            {
                return Optional.empty();
            }
        });
    }

    private static String describe(List<AnnotationNode> points, List<AnnotationNode> constants)
    {
        List<String> parts = new ArrayList<>();
        for (AnnotationNode point : points)
            parts.add(value(point, "value") + (value(point, "target") == null ? "" : " " + value(point, "target")));
        for (AnnotationNode constant : constants)
            parts.add("constant " + constant.values);
        return String.join(", ", parts);
    }

    /** Mixin's own annotations differ in retention, so both lists are searched. */
    private static List<AnnotationNode> annotations(List<AnnotationNode> visible, List<AnnotationNode> invisible)
    {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (visible != null)
            annotations.addAll(visible);
        if (invisible != null)
            annotations.addAll(invisible);
        return annotations;
    }

    private static AnnotationNode annotation(List<AnnotationNode> visible, List<AnnotationNode> invisible, String descriptor)
    {
        return annotations(visible, invisible).stream().filter(annotation -> annotation.desc.equals(descriptor)).findFirst().orElse(null);
    }

    private static Object value(AnnotationNode annotation, String key)
    {
        if (annotation.values == null)
            return null;
        for (int index = 0; index + 1 < annotation.values.size(); index += 2)
        {
            if (annotation.values.get(index).equals(key))
                return annotation.values.get(index + 1);
        }
        return null;
    }

    private static List<?> list(Object value)
    {
        if (value == null)
            return List.of();
        return value instanceof List<?> values ? values : List.of(value);
    }

    private static String decapitalize(String name)
    {
        return name.isEmpty() ? name : Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
