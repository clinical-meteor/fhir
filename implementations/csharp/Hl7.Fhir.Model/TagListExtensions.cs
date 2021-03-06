﻿/* 
 * Copyright (c) 2014, Furore (info@furore.com) and contributors
 * See the file CONTRIBUTORS for details.
 * 
 * This file is licensed under the BSD 3-Clause license
 * available at https://raw.githubusercontent.com/ewoutkramer/fhir-net-api/master/LICENSE
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Hl7.Fhir.Model;
using System.IO;

using Hl7.Fhir.Introspection;
using System.ComponentModel.DataAnnotations;
using Hl7.Fhir.Support;

namespace Hl7.Fhir.Model
{
    public enum BundleType
    {
        Unspecified,
        Document,
        Message
    }

    public static class TagListExtensions
    {
        public static readonly Uri FHIRTAGSCHEME_GENERAL = new Uri(XmlNs.FHIRTAG, UriKind.Absolute);
        public static readonly Uri FHIRTAGSCHEME_PROFILE = new Uri(XmlNs.TAG_PROFILE, UriKind.Absolute);
        public static readonly Uri FHIRTAGSCHEME_SECURITY = new Uri(XmlNs.TAG_SECURITY, UriKind.Absolute);

        public const string TAG_TERM_DOCUMENT = "http://hl7.org/fhir/tag/document";
        public const string TAG_TERM_MESSAGE = "http://hl7.org/fhir/tag/message";
        public const string TAG_TERM_TEXT = "http://hl7.org/fhir/tag/text/";

        private static readonly Coding MESSAGE_TAG = new Coding(XmlNs.FHIRTAG, TAG_TERM_MESSAGE);
        private static readonly Coding DOCUMENT_TAG = new Coding(XmlNs.FHIRTAG, TAG_TERM_DOCUMENT);

        public static void SetTextTag(this Resource entry, string text)
        {
            if (entry.Meta == null) entry.Meta = new Resource.ResourceMetaComponent();
            if (entry.Meta.Tag == null) entry.Meta.Tag = new List<Coding>();

            entry.Meta.Tag.RemoveAll(t => Equals(t.System, XmlNs.FHIRTAG) &&
                    (t.Code != null && t.Code.StartsWith(TAG_TERM_TEXT)));

            entry.Meta.Tag.Add(new Coding(XmlNs.FHIRTAG, TAG_TERM_TEXT + Uri.EscapeUriString(text)) { Display = text });
        }

        public static string GetTextTag(this Resource entry)
        {
            if (entry.Meta == null) entry.Meta = new Resource.ResourceMetaComponent();
            if (entry.Meta.Tag == null) entry.Meta.Tag = new List<Coding>();

            var textTag = entry.Meta.Tag.FilterByScheme(XmlNs.FHIRTAG).
                Where(t => t.Term.StartsWith(TAG_TERM_TEXT)).SingleOrDefault();

            if (textTag == null) return null;

            return Uri.UnescapeDataString(textTag.Term.Substring(TAG_TERM_TEXT.Length));
        }

        public static void SetBundleType(this Bundle bundle, BundleType type)
        {
            List<Tag> result = new List<Tag>(bundle.Tags.Exclude(new Tag[] { MESSAGE_TAG, DOCUMENT_TAG }));

            if (type == BundleType.Document)
                result.Add(DOCUMENT_TAG);
            else if (type == BundleType.Message)
                result.Add(MESSAGE_TAG);

            bundle.Tags = result;
        }

        public static BundleType GetBundleType(this Bundle bundle)
        {
            if (bundle.Tags.Contains(DOCUMENT_TAG))
                return BundleType.Document;
            else if (bundle.Tags.Contains(MESSAGE_TAG))
                return BundleType.Message;
            else
                return BundleType.Unspecified;
        }

        public static IEnumerable<Tag> FilterByScheme(this IEnumerable<Tag> tags, Uri scheme)
        {
            if (scheme == null) Error.ArgumentNull("scheme");

            return tags.Where(e => Uri.Equals(e.Scheme, scheme));
        }

        public static bool UriIsFhirScheme(Uri scheme)
        {
            return
                Uri.Equals(scheme, Tag.FHIRTAGSCHEME_GENERAL)
                || Uri.Equals(scheme, Tag.FHIRTAGSCHEME_PROFILE)
                || Uri.Equals(scheme, Tag.FHIRTAGSCHEME_SECURITY);
        }

        public static bool HasFhirScheme(this Tag tag)
        {
            return UriIsFhirScheme(tag.Scheme);
        }

        public static IEnumerable<Tag> FilterOnFhirSchemes(this IEnumerable<Tag> tags)
        {
            return tags.Where(e => e.HasFhirScheme());
        }

        public static IEnumerable<Tag> Exclude(this IEnumerable<Tag> tags, IEnumerable<Tag> that)
        {
            if (that == null) Error.ArgumentNull("that");

            return tags.Where(t => !that.Contains(t)); 
        }

        public static IEnumerable<Tag> Exclude(this IEnumerable<Tag> tags, Tag tag)
        {
            if (tag == null) Error.ArgumentNull("tag");

            return tags.Where(t => !Equals(t,tag));
        }

    }
}

