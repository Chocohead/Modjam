package com.chocohead.sm.loader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.commons.lang3.StringUtils;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.util.version.VersionParsingException;

import com.chocohead.sm.api.SaltsModMetadata;
import com.chocohead.sm.impl.SortedModDependency.Ordered;
import com.chocohead.sm.loader.ModBuilder.ContactBuilder;
import com.chocohead.sm.loader.ModBuilder.PersonBuilder;

@PreMixinClassloaded
public class ModParser {
	public static Set<ModMetadata> read(InputStream xml, EnvType side) throws IOException {
		Set<ModNode> readMods;
		try {
			readMods = create(xml);
		} catch (SAXException e) {
			throw new IllegalArgumentException("Invalid mod XML", e);
		}

		Set<ModMetadata> mods = new HashSet<>();
		for (ModNode mod : readMods) {
			ModBuilder builder = new ModBuilder(mod.id, mod.version);

			for (Node child : mod.children) {
				switch (child.getType()) {
				case name:
					builder.withName(((ModNameNode) child).name);
					break;

				case description:
					builder.withDescription(((DescriptionNode) child).description);
					break;

				case license:
					builder.withLicense(((LicenseNode) child).name);
					break;

				case icon:
					builder.withIcon(((IconNode) child).iconPath);
					break;

				case listeners:
					for (Node grandChild : ((ParentNode) child).children) {
						if (grandChild.getType() == NodeType.type) {
							ListenerTypeNode node = (ListenerTypeNode) grandChild;

							for (Node greatGrandChild : node.children) {
								if (greatGrandChild.getType() == NodeType.listener) {
									ListenerNode deeperNode = (ListenerNode) greatGrandChild;
									if (!deeperNode.side.loadedSides.contains(side)) continue; //Wrong side

									builder.withListener(node.type, deeperNode.className);
								} else {
									assert NodeType.type.validChildren.contains(greatGrandChild.getType());
									throw new IllegalStateException("Unexpected child element in (listener) type node: " + greatGrandChild);
								}
							}
						} else {
							assert NodeType.listeners.validChildren.contains(grandChild.getType());
							throw new IllegalStateException("Unexpected child element in listeners node: " + grandChild);
						}
					}
					break;

				case dependencies:
					for (Node grandChild : ((ParentNode) child).children) {
						if (grandChild.getType() == NodeType.dependency) {
							DependencyNode node = (DependencyNode) grandChild;

							if (node.order == Sorting.INDIFFERENT) {
								builder.withDependency(node.id, node.type);
							} else {
								builder.withOrderedDependency(node.id, node.type, node.order.asOrdering());
							}
						} else {
							assert NodeType.dependencies.validChildren.contains(grandChild.getType());
							throw new IllegalStateException("Unexpected child element in dependencies node: " + grandChild);
						}
					}
					break;

				case suggestions:
					for (Node grandChild : ((ParentNode) child).children) {
						if (grandChild.getType() == NodeType.suggestion) {
							SuggestionNode node = (SuggestionNode) grandChild;

							if (node.order == Sorting.INDIFFERENT) {
								builder.withSuggestion(node.id, node.type, node.strong);
							} else {
								builder.withOrderedSuggestion(node.id, node.type, node.order.asOrdering(), node.strong);
							}
						} else {
							assert NodeType.suggestions.validChildren.contains(grandChild.getType());
							throw new IllegalStateException("Unexpected child element in suggestions node: " + grandChild);
						}
					}
					break;

				case conflicts:
					for (Node grandChild : ((ParentNode) child).children) {
						if (grandChild.getType() == NodeType.conflict) {
							ConflictNode node = (ConflictNode) grandChild;

							if (node.order == Sorting.INDIFFERENT) {
								builder.withConflict(node.id, node.type, node.severe);
							} else {
								builder.withOrderedConflict(node.id, node.type, node.order.asOrdering(), node.severe);
							}
						} else {
							assert NodeType.conflicts.validChildren.contains(grandChild.getType());
							throw new IllegalStateException("Unexpected child element in conflicts node: " + grandChild);
						}
					}
					break;

				case mixins:
					for (Node grandChild : ((ParentNode) child).children) {
						if (grandChild.getType() == NodeType.config) {
							builder.withMixinConfig(((MixinConfigNode) grandChild).config);
						} else {
							assert NodeType.mixins.validChildren.contains(grandChild.getType());
							throw new IllegalStateException("Unexpected child element in mixins node: " + grandChild);
						}
					}
					break;

				case project: {
					ContactBuilder projectBuilder = builder.withProjectContacts();

					for (Node grandChild : ((ParentNode) child).children) {
						switch (grandChild.getType()) {
						case issues:
							projectBuilder.withIssues(((ModIssuesNode) grandChild).issues);
							break;

						case source:
							projectBuilder.withSource(((ModSourceNode) grandChild).source);
							break;

						case homepage:
							projectBuilder.withHomepage(((HomepageNode) grandChild).url);
							break;

						case irc:
							projectBuilder.withIRC(((IRCNode) grandChild).url);
							break;

						case discord:
							projectBuilder.withDiscord(((DiscordInviteNode) grandChild).inviteCode);
							break;

						default:
							assert NodeType.project.validChildren.contains(grandChild.getType());
							throw new IllegalStateException("Unexpected child element in project node: " + grandChild);
						}
					}

					projectBuilder.butNotMore();
					break;
				}

				case authors:
				case contributors:
					for (Node grandChild : ((ParentNode) child).children) {
						PersonNode node;
						PersonBuilder personBuilder;
						switch (child.getType()) {
						case authors:
							if (grandChild.getType() == NodeType.author) {
								node = (PersonNode) grandChild;
								personBuilder = builder.withAuthor(node.name);
							} else {
								assert NodeType.authors.validChildren.contains(grandChild.getType());
								throw new IllegalStateException("Unexpected child element in authors node: " + grandChild);
							}
							break;

						case contributors:
							if (grandChild.getType() == NodeType.contributor) {
								node = (PersonNode) grandChild;
								personBuilder = builder.withContributor(node.name);
							} else {
								assert NodeType.contributors.validChildren.contains(grandChild.getType());
								throw new IllegalStateException("Unexpected child element in contributors node: " + grandChild);
							}
							break;

						default:
							assert child.getType().validChildren.contains(grandChild.getType());
							throw new IllegalStateException("Unexpected child element in " + child.getType() + " node: " + grandChild);
						}

						for (Node greatGrandChild : node.children) {
							switch (greatGrandChild.getType()) {
							case homepage:
								personBuilder.withHomepage(((HomepageNode) greatGrandChild).url);
								break;

							case email:
								personBuilder.withEmail(((EmailNode) greatGrandChild).email);
								break;

							case irc:
								personBuilder.withIRC(((IRCNode) greatGrandChild).url);
								break;

							case twitter:
								personBuilder.withTwitter(((TwitterNode) greatGrandChild).handle);
								break;

							case discord:
								personBuilder.withDiscord(((DiscordUsernameNode) greatGrandChild).username);
								break;

							default:
								assert grandChild.getType().validChildren.contains(greatGrandChild.getType());
								throw new IllegalStateException("Unexpected child element in " + grandChild.getType() + " node: " + greatGrandChild);
							}
						}

						personBuilder.butNotMore();
					}
					break;

				case custom:
					for (Node grandChild : ((ParentNode) child).children) {
						if (grandChild.getType() == NodeType.entry) {
							CustomEntryNode node = (CustomEntryNode) grandChild;
							builder.withCustomData(node.key, node.value);
						} else {
							assert NodeType.custom.validChildren.contains(grandChild.getType());
							throw new IllegalStateException("Unexpected child element in custom node: " + grandChild);
						}
					}
					break;

				default:
					assert NodeType.mod.validChildren.contains(child.getType());
					throw new IllegalStateException("Unexpected child element in mod node: " + child);
				}
			}

			mods.add(builder.build());
		}

		return mods;
	}

	private static Set<ModNode> create(InputStream xml) throws SAXException, IOException {
		try (InputStream in = xml instanceof BufferedInputStream ? xml : new BufferedInputStream(xml)) {
			XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();

			SaxHandler handler = new SaxHandler();
			reader.setContentHandler(handler);
			reader.parse(new InputSource(in));

			return handler.getResult();
		} catch (ParserConfigurationException e) {
			throw new AssertionError("Unexpected problem occurred creating XML parser", e);
		}
	}

	private static class SaxHandler extends DefaultHandler {
		private ParentNode parentNode;
		private Node currentNode;

		SaxHandler() {
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			NodeType type = NodeType.get(qName);
			if (type == null) type = NodeType.get(qName.toLowerCase(Locale.ENGLISH));
			if (type == null) throw new SAXException("Invalid/Unknown element: " + qName);

			if (parentNode == null ? type != NodeType.mod && type != NodeType.mods
					: type == NodeType.mods || type == NodeType.mod && parentNode.getType() != NodeType.mods) {
				throw new SAXException("Invalid " + type + " element location");
			}

			switch (type) {
			case mods:
				currentNode = parentNode = new ModsNode();
				break;

			case mod:
				if (parentNode != null) {
					currentNode = new ModNode(attributes);
					parentNode.addNode(currentNode);
					parentNode = (ParentNode) currentNode;
				} else {
					currentNode = parentNode = new ModNode(attributes);
				}
				break;

			case listeners:
			case dependencies:
			case suggestions:
			case conflicts:
			case mixins:
			case contact:
			case project:
			case authors:
			case contributors:
			case custom:
				currentNode = new GenericParentNode(parentNode, type);
				parentNode.addNode(currentNode);
				parentNode = (ParentNode) currentNode;
				break;

			case listener:
				currentNode = new ListenerNode(parentNode, attributes);
				parentNode.addNode(currentNode);
				break;

			case type:
				currentNode = new ListenerTypeNode(parentNode, attributes);
				parentNode.addNode(currentNode);
				parentNode = (ParentNode) currentNode;
				break;

			case dependency:
				currentNode = new DependencyNode(parentNode, attributes);
				parentNode.addNode(currentNode);
				break;

			case suggestion:
				currentNode = new SuggestionNode(parentNode, attributes);
				parentNode.addNode(currentNode);
				break;

			case conflict:
				currentNode = new ConflictNode(parentNode, attributes);
				parentNode.addNode(currentNode);
				break;

			case config:
				currentNode = new MixinConfigNode(parentNode, attributes);
				parentNode.addNode(currentNode);
				break;

			case homepage:
				currentNode = new HomepageNode(parentNode);
				parentNode.addNode(currentNode);
				break;

			case email:
				currentNode = new EmailNode(parentNode);
				parentNode.addNode(currentNode);
				break;

			case irc:
				currentNode = new IRCNode(parentNode);
				parentNode.addNode(currentNode);
				break;

			case twitter:
				currentNode = new TwitterNode(parentNode);
				parentNode.addNode(currentNode);
				break;

			case discord:
				assert parentNode.getType() == NodeType.contact || parentNode.getType() == NodeType.project;
				currentNode = parentNode.getType() == NodeType.contact ? new DiscordUsernameNode(parentNode) : new DiscordInviteNode(parentNode);
				parentNode.addNode(currentNode);
				break;

			case issues:
				currentNode = new ModIssuesNode(parentNode);
				parentNode.addNode(currentNode);
				break;

			case source:
				currentNode = new ModSourceNode(parentNode);
				parentNode.addNode(currentNode);
				break;

			case author:
			case contributor:
				currentNode = new PersonNode(parentNode, type, attributes);
				parentNode.addNode(currentNode);
				parentNode = (ParentNode) currentNode;
				break;

			case entry:
				currentNode = new CustomEntryNode(parentNode, attributes);
				parentNode.addNode(currentNode);
				break;

			case name:
				currentNode = new ModNameNode(parentNode);
				parentNode.addNode(currentNode);
				break;

			case description:
				currentNode = new DescriptionNode(parentNode);
				parentNode.addNode(currentNode);
				break;

			case license:
				currentNode = new LicenseNode(parentNode);
				parentNode.addNode(currentNode);
				break;

			case icon:
				currentNode = new IconNode(parentNode);
				parentNode.addNode(currentNode);
				break;
			}
		}

		@Override
		public void characters(char[] chars, int start, int length) throws SAXException {
			while (length > 0 && Character.isWhitespace(chars[start])) {
				start++;
				length--;
			}

			while (length > 0 && Character.isWhitespace(chars[start + length - 1])) {
				length--;
			}

			if (length != 0) {
				if (currentNode == null) throw new SAXException("Unexpected characters out of element");

				currentNode.setContent(new String(chars, start, length));
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			currentNode.end();

			if (currentNode == parentNode) {
				if (currentNode.getType() == NodeType.mod) {
					currentNode = null;
				} else {
					currentNode = parentNode = parentNode.parent;
				}
			} else {
				currentNode = parentNode;
			}
		}

		public Set<ModNode> getResult() {
			return ((ModsNode) parentNode).getMods();
		}
	}

	private enum NodeType {
		listener, //Wraps class
		type(listener), //Has listener type
		listeners(type),

		dependency, //Has id, version and optional
		dependencies(dependency),
		suggestion, //Has id, version and strong
		suggestions(suggestion),
		conflict, //Has id, version and severe
		conflicts(conflict),

		config, //Wraps Mixin configuration JSON
		mixins(config),

		homepage,
		email,
		irc,
		twitter,
		discord,
		contact(homepage, email, irc, twitter, discord),

		issues,
		source,
		project(issues, source, homepage, irc, discord),

		author(contact), //Has name
		authors(author),
		contributor(contact), //Has name
		contributors(contributor),

		entry, //Has key, wraps value
		custom(entry),

		name,
		description,
		license,
		icon,

		mod(name, description, license, icon, listeners, dependencies, suggestions, conflicts, mixins, project, authors, contributors, custom), //Has id and version
		mods(mod);

		private NodeType(NodeType... types) {
			this.types = types;
		}

		private static final Map<String, NodeType> MAP = Arrays.stream(values()).collect(Collectors.toMap(NodeType::name, Function.identity()));
		public static NodeType get(String name) {
			return MAP.get(name);
		}

		private NodeType[] types;
		Set<NodeType> validChildren; //Such a shame it can't be final
		static {
			for (NodeType type : values()) {
				type.validChildren = type.types.length == 0 ? Collections.emptySet() : EnumSet.copyOf(Arrays.asList(type.types));
				type.types = null;
			}
		}
	}

	private static abstract class Node {
		final ParentNode parent;

		protected Node(ParentNode parent) {
			this.parent = parent;
		}

		public abstract NodeType getType();

		void setContent(String content) throws SAXException {
			throw new SAXException("Unexpected characters in " + getType());
		}

		void end() throws SAXException {
		}

		@Override
		public String toString() {
			return "Node<" + getType() + '>';
		}

		protected static Optional<String> getOptionalAttribute(Attributes attributes, String name) {
			return Optional.ofNullable(attributes.getValue(name));
		}

		protected static String getAttribute(Attributes attributes, String name) throws SAXException {
			return getOptionalAttribute(attributes, name).orElseThrow(missingAttribute(name));
		}

		protected static Supplier<SAXException> missingAttribute(String name) {
			return () -> new SAXException("Missing attribute: " + name);
		}

		protected static boolean getBoolAttribute(Attributes attributes, String name) throws SAXException {
			String value = getAttribute(attributes, name);

			if ("true".equalsIgnoreCase(value)) {
				return true;
			} else if ("false".equalsIgnoreCase(value)) {
				return false;
			} else {
				throw new SAXException("Invalid boolean value: " + value + " from attribute " + name);
			}
		}
	}

	private static abstract class ParentNode extends Node {
		protected final List<Node> children = new ArrayList<>();

		protected ParentNode(ParentNode parent) {
			super(parent);
		}

		void addNode(Node node) throws SAXException {
			if (!getType().validChildren.contains(node.getType())) throw new SAXException("Invalid child: " + node + " for " + this);
			children.add(node);
		}

		@Override
		public String toString() {
			return "ParentNode<" + getType() + ": " + children + '>';
		}
	}

	private static final class GenericParentNode extends ParentNode {
		private final NodeType type;

		GenericParentNode(ParentNode parent, NodeType type) {
			super(parent);

			this.type = type;
		}

		@Override
		public NodeType getType() {
			return type;
		}
	}

	private static class ModsNode extends ParentNode {
		protected ModsNode(ParentNode node) {
			super(node);
			assert getClass() != ModsNode.class;
		}

		ModsNode() {
			super(null);
			assert getClass() == ModsNode.class;
		}

		Set<ModNode> getMods() {
			return children.stream().map(ModNode.class::cast).collect(Collectors.toSet());
		}

		@Override
		public NodeType getType() {
			return NodeType.mod;
		}
	}

	private static final class ModNode extends ModsNode {
		final String id;
		final SemanticVersion version;

		ModNode(Attributes attributes) throws SAXException {
			this(null, attributes);
		}

		ModNode(ParentNode node, Attributes attributes) throws SAXException {
			super(node);

			id = getAttribute(attributes, "id");
			try {
				version = SemanticVersion.parse(getAttribute(attributes, "version"));
			} catch (VersionParsingException e) {
				throw new SAXException("Invalid mod version for " + id + " specified: " + getAttribute(attributes, "version"), e);
			}
		}

		@Override
		Set<ModNode> getMods() {
			return Collections.singleton(this);
		}

		@Override
		public NodeType getType() {
			return NodeType.mod;
		}
	}

	private static final class IconNode extends Node {
		String iconPath;

		IconNode(ParentNode node) {
			super(node);
		}

		@Override
		void setContent(String content) throws SAXException {
			iconPath = content;
		}

		@Override
		void end() throws SAXException {
			if (StringUtils.isBlank(iconPath)) {
				throw new SAXException("Icon specified without path");
			}
		}

		@Override
		public NodeType getType() {
			return NodeType.icon;
		}
	}

	private static final class LicenseNode extends Node {
		String name;

		LicenseNode(ParentNode node) {
			super(node);
		}

		@Override
		void setContent(String content) throws SAXException {
			name = content;
		}

		@Override
		void end() throws SAXException {
			if (StringUtils.isBlank(name)) {
				throw new SAXException("License specified without name");
			}
		}

		@Override
		public NodeType getType() {
			return NodeType.license;
		}
	}

	private static final class DescriptionNode extends Node {
		String description;

		DescriptionNode(ParentNode node) {
			super(node);
		}

		@Override
		void setContent(String content) throws SAXException {
			description = content;
		}

		@Override
		void end() throws SAXException {
			//Having a description isn't vital, the tag can be empty
			if (StringUtils.isBlank(description)) description = null;
		}

		@Override
		public NodeType getType() {
			return NodeType.description;
		}
	}

	private static final class ModNameNode extends Node {
		String name;

		ModNameNode(ParentNode node) {
			super(node);
		}

		@Override
		void setContent(String content) throws SAXException {
			name = content;
		}

		@Override
		void end() throws SAXException {
			if (StringUtils.isBlank(name)) {
				throw new SAXException("Mod name specified without an actual name");
			}
		}

		@Override
		public NodeType getType() {
			return NodeType.name;
		}
	}

	private static final class CustomEntryNode extends Node {
		final String key;
		String value;

		CustomEntryNode(ParentNode node, Attributes attributes) throws SAXException {
			super(node);

			key = getAttribute(attributes, "key");
		}

		@Override
		void setContent(String content) throws SAXException {
			value = content;
		}

		@Override
		public NodeType getType() {
			return NodeType.entry;
		}
	}

	private static final class PersonNode extends ParentNode {
		private final NodeType type;
		final String name;

		PersonNode(ParentNode node, NodeType type, Attributes attributes) throws SAXException {
			super(node);

			this.type = type;
			name = getAttribute(attributes, "name");
		}

		@Override
		public NodeType getType() {
			return type;
		}
	}

	private static final class ModSourceNode extends Node {
		URL source;

		ModSourceNode(ParentNode node) {
			super(node);
		}

		@Override
		void setContent(String content) throws SAXException {
			try {
				source = new URL(content);
			} catch (MalformedURLException e) {
				throw new SAXException("Invalid URL specified for mod source: " + content, e);
			}
		}

		@Override
		void end() throws SAXException {
			if (source == null) {
				throw new SAXException("Mod source specified without a URL");
			}
		}

		@Override
		public NodeType getType() {
			return NodeType.source;
		}
	}

	private static final class ModIssuesNode extends Node {
		URL issues;

		ModIssuesNode(ParentNode node) {
			super(node);
		}

		@Override
		void setContent(String content) throws SAXException {
			try {
				issues = new URL(content);
			} catch (MalformedURLException e) {
				throw new SAXException("Invalid URL specified for mod issues: " + content, e);
			}
		}

		@Override
		void end() throws SAXException {
			if (issues == null) {
				throw new SAXException("Mod issues specified without a URL");
			}
		}

		@Override
		public NodeType getType() {
			return NodeType.issues;
		}
	}

	private static final class DiscordInviteNode extends Node {
		String inviteCode;

		DiscordInviteNode(ParentNode node) {
			super(node);
		}


		@Override
		void setContent(String content) throws SAXException {
			inviteCode = TwitterNode.cutHandle(content);
		}

		@Override
		void end() throws SAXException {
			if (StringUtils.isBlank(inviteCode)) {
				throw new SAXException("Discord invite specified without code");
			}
		}

		@Override
		public NodeType getType() {
			return NodeType.discord;
		}
	}

	private static final class DiscordUsernameNode extends Node {
		String username;

		DiscordUsernameNode(ParentNode node) {
			super(node);
		}


		@Override
		void setContent(String content) throws SAXException {
			username = content;
		}

		@Override
		void end() throws SAXException {
			if (StringUtils.isBlank(username)) {
				throw new SAXException("Discord username specified without name");
			}
		}

		@Override
		public NodeType getType() {
			return NodeType.discord;
		}
	}

	private static final class TwitterNode extends Node {
		String handle;

		TwitterNode(ParentNode node) {
			super(node);
		}

		static String cutHandle(String content) {
			assert content.equals(content.trim());
			int split = content.lastIndexOf('/');

			if (split >= 0) {//We just want the invite code, not the whole URL
				if (split == content.length() - 1) {
					split = content.lastIndexOf('/', split - 1);
					return content.substring(split + 1, content.length() - 1);
				} else {
					return content.substring(split + 1);
				}
			} else {
				return content;
			}
		}

		@Override
		void setContent(String content) throws SAXException {
			handle = cutHandle(content);
		}

		@Override
		void end() throws SAXException {
			if (StringUtils.isBlank(handle)) {
				throw new SAXException("Twitter handle specified without actual handle");
			}
		}

		@Override
		public NodeType getType() {
			return NodeType.twitter;
		}
	}

	private static final class IRCNode extends Node {
		URI url;

		IRCNode(ParentNode node) {
			super(node);
		}

		@Override
		void setContent(String content) throws SAXException {
			try {
				url = new URI(content);
			} catch (URISyntaxException e) {
				throw new SAXException("Invalid URL specified for irc: " + content, e);
			}
		}

		@Override
		void end() throws SAXException {
			if (url == null) {
				throw new SAXException("IRC specified without a URL");
			}
		}

		@Override
		public NodeType getType() {
			return NodeType.irc;
		}
	}

	private static final class EmailNode extends Node {
		URL email;

		EmailNode(ParentNode node) {
			super(node);
		}

		@Override
		void setContent(String content) throws SAXException {
			try {
				email = new URL(content.startsWith("mailto:") ? content : "mailto:".concat(content));
			} catch (MalformedURLException e) {
				throw new SAXException("Invalid address specified for email: " + content, e);
			}
		}

		@Override
		void end() throws SAXException {
			if (email == null) {
				throw new SAXException("Email specified without an address");
			}
		}

		@Override
		public NodeType getType() {
			return NodeType.email;
		}
	}

	private static final class HomepageNode extends Node {
		URL url;

		HomepageNode(ParentNode node) {
			super(node);
		}

		@Override
		void setContent(String content) throws SAXException {
			try {
				url = new URL(content);
			} catch (MalformedURLException e) {
				throw new SAXException("Invalid URL specified for homepage: " + content, e);
			}
		}

		@Override
		void end() throws SAXException {
			if (url == null) {
				throw new SAXException("Homepage specified without a URL");
			}
		}

		@Override
		public NodeType getType() {
			return NodeType.homepage;
		}
	}

	private static final class MixinConfigNode extends Node {
		final String config;

		MixinConfigNode(ParentNode node, Attributes attributes) throws SAXException {
			super(node);

			config = getAttribute(attributes, "file");
		}

		@Override
		public NodeType getType() {
			return NodeType.config;
		}
	}

	private enum Sorting {
		BEFORE(Ordered.BEFORE), INDIFFERENT(null), AFTER(Ordered.AFTER);

		private Sorting(Ordered ordering) {
			this.ordering = ordering;
		}

		Ordered asOrdering() {
			if (ordering == null) throw new UnsupportedOperationException("Cannot convert " + this + " to ordering");
			return ordering;
		}

		private final Ordered ordering;

		static Sorting get(String value) throws SAXException {
			for (Sorting preference : Sorting.values()) {
				if (value.equalsIgnoreCase(preference.name())) {
					return preference;
				}
			}

			throw new SAXException("Invalid sorting preference: " + value);
		}
	}

	private static class DependencyNode extends Node {
		final String id;
		//final Version version;
		final String type;
		final Sorting order;

		DependencyNode(ParentNode node, Attributes attributes) throws SAXException {
			super(node);

			id = getAttribute(attributes, "id");
			/*String version = getOptionalAttribute(attributes, "version").orElse(null);
			if (version != null) {
				try {
					this.version = Version.parse(version);
				} catch (VersionParsingException e) {
					throw new SAXException("Invalid mod dependency version for " + id + " specified: " + getAttribute(attributes, "version"), e);
				}
			} else {
				this.version = null;
			}*/
			type = getOptionalAttribute(attributes, "type").orElse(SaltsModMetadata.TYPE);
			order = Sorting.get(getOptionalAttribute(attributes, "loading").orElse(Sorting.INDIFFERENT.name()));
		}

		@Override
		public NodeType getType() {
			return NodeType.dependency;
		}
	}

	private static final class SuggestionNode extends DependencyNode {
		final boolean strong;

		SuggestionNode(ParentNode node, Attributes attributes) throws SAXException {
			super(node, attributes);

			boolean strong;
			try {
				strong = getBoolAttribute(attributes, "strong");
			} catch (SAXException e) {
				strong = false;
			}
			this.strong = strong;
		}

		@Override
		public NodeType getType() {
			return NodeType.suggestion;
		}
	}

	private static final class ConflictNode extends DependencyNode {
		final boolean severe;

		ConflictNode(ParentNode node, Attributes attributes) throws SAXException {
			super(node, attributes);

			boolean severe;
			try {
				severe = getBoolAttribute(attributes, "severe");
			} catch (SAXException e) {
				severe = false;
			}
			this.severe = severe;
		}

		@Override
		public NodeType getType() {
			return NodeType.conflict;
		}
	}

	private static final class ListenerTypeNode extends ParentNode {
		final String type;

		ListenerTypeNode(ParentNode node, Attributes attributes) throws SAXException {
			super(node);

			type = getAttribute(attributes, "class");
		}

		@Override
		public NodeType getType() {
			return NodeType.type;
		}
	}

	private enum Side {
		CLIENT(EnvType.CLIENT), SERVER(EnvType.SERVER), BOTH(EnvType.CLIENT, EnvType.SERVER);

		private Side(EnvType loadedSide) {
			loadedSides = Collections.unmodifiableSet(EnumSet.of(loadedSide));
		}

		private Side(EnvType loadedSideA, EnvType loadedSideB) {
			loadedSides = Collections.unmodifiableSet(EnumSet.of(loadedSideA, loadedSideB));
		}

		public final Set<EnvType> loadedSides;

		static Side get(String value) throws SAXException {
			for (Side side : Side.values()) {
				if (value.equalsIgnoreCase(side.name())) {
					return side;
				}
			}

			throw new SAXException("Invalid side: " + value);
		}
	}

	private static final class ListenerNode extends Node {
		final Side side;
		String className;

		ListenerNode(ParentNode node, Attributes attributes) throws SAXException {
			super(node);

			side = Side.get(getOptionalAttribute(attributes, "side").orElse(Side.BOTH.name()));
		}

		@Override
		void setContent(String content) throws SAXException {
			className = content;
		}

		@Override
		void end() throws SAXException {
			if (StringUtils.isBlank(className)) {
				throw new SAXException("Listener specified without a class");
			}
		}

		@Override
		public NodeType getType() {
			return NodeType.listener;
		}
	}
}