# Gyfu

( [Changelog] | **[API]** )

Gyfu «ᚷ» aims to become an [ISO Schematron][schematron] implementation in Clojure.

In other words, Gyfu is a library for testing that your XML document has the things it should have and doesn't have the things it shouldn't.
 
For an example, [see here][example].

**Gyfu is currently unreleased and under development**.

## vs. Schematron

Compared to [the XSLT Schematron implementation][skeleton], Gyfu aims to offer these benefits:

- Better performance
- A more pleasant schema authoring experience (if you're more into S-expressions than angle brackets, anyway)
- Writing tests in Clojure in addition to XPath
- XPath 3.1 support out of the box

## TODO

- [ ] Support the `diagnostics` element
- [ ] Command-line interface
- [ ] Parse Schematron schema from `.sch` XML file
- [ ] Console output
- [ ] HTML output
- [ ] Support XSLT keys (if possible)
- [ ] Support custom XPath functions

At the moment, I'm not sure about supporting abstract patterns. I'm thinking you could just use a regular Clojure function and construct XPath expressions dynamically.

## License

Copyright © 2017 Eero Helenius.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[API]: https://eerohele.github.io/gyfu/
[CHANGELOG]: https://github.com/eerohele/gyfu/blob/master/CHANGELOG.md

[example]: http://github.com/eerohele/gyfu/blob/master/src/gyfu/examples/pain_mdr.clj
[schematron]: http://schematron.com
[skeleton]: http://schematron.com/front-page/the-schematron-skeleton-implementation
