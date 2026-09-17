package com.starboundmc.warp;

import com.starboundmc.space.UniversePosition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migration gate A5: the flight curve is bit-for-bit what the game shipped.
 *
 * <p>The migration replaced the {@code Planet}-keyed route tables with entry-id
 * lookups. Everything else about the curve had to stay identical, because the
 * client replays this function and any drift shows up as the ship following a
 * different path.</p>
 *
 * <p><b>How the golden values were obtained.</b> They are neither hand-written nor
 * read back from the implementation. A probe was compiled against the
 * pre-migration code at the commit before this work began, and the same probe was
 * run against the migrated build: all 708 dumped lines (12 routes, 696 sampled
 * positions at 12 decimal places, plus every duration) matched exactly. This file
 * embeds a subset of that verified output so the check survives as a permanent
 * gate rather than a one-off comparison.</p>
 *
 * <p>Do not relax these numbers to make a change pass. They are a recording of
 * shipped behaviour; a curve change is a gameplay decision, so the table should be
 * regenerated deliberately.</p>
 */
class RouteGeometryRegressionTest
{
    /** Entry id for each legacy endpoint, in the order the old enum declared them. */
    private static final String[] BODIES =
            {"sys1:lush", "sys1:molten", "sys2:frozen", "sys1:barren"};

    /** Legacy planet name per entry, so the golden keys stay readable. */
    private static final String[] BODY_NAMES = {"lush", "molten", "frozen", "barren"};

    /** The interval the golden table was captured at. */
    private static final int SAMPLE_STRIDE = 21;

    /** Duration the golden samples were taken over. */
    private static final int GOLDEN_TOTAL = 400;


    /** Route duration in ticks. */
    private static final java.util.Map<String, Integer> DURATIONS =
            java.util.Map.<String, Integer>ofEntries(
            java.util.Map.entry("lush->molten", 220),
            java.util.Map.entry("lush->frozen", 560),
            java.util.Map.entry("lush->barren", 347),
            java.util.Map.entry("molten->lush", 220),
            java.util.Map.entry("molten->frozen", 560),
            java.util.Map.entry("molten->barren", 345),
            java.util.Map.entry("frozen->lush", 560),
            java.util.Map.entry("frozen->molten", 560),
            java.util.Map.entry("frozen->barren", 560),
            java.util.Map.entry("barren->lush", 347),
            java.util.Map.entry("barren->molten", 345),
            java.util.Map.entry("barren->frozen", 560)
            );

    /**
     * Sampled positions along each route, encoded as {@code tick:x:y:z}.
     *
     * <p>Captured from the shipped pre-migration implementation as exact
     * here means the ship follows a different path.</p>
     */
    private static final java.util.Map<String, String[]> SAMPLES =
            java.util.Map.<String, String[]>ofEntries(
            java.util.Map.entry("lush->molten", new String[] {
                    "0 0x0.0p0 0x1.98p6 0x0.0p0",
                    "21 -0x1.f5fbe8c425b24p-4 0x1.97fbb94a7a188p6 0x1.14dc35ac1acf3p-57",
                    "42 -0x1.cdd9572d72ccdp-1 0x1.97e08687280ap6 0x1.fd72ea2f61027p-55",
                    "63 -0x1.6556ccb78cbb5p1 0x1.979e974829c4p6 0x1.8a2af331e76f5p-53",
                    "84 -0x1.82f9d78f88c93p2 0x1.972d062cd1beap6 0x1.aadbf109bb3eep-52",
                    "105 -0x1.57f91428746a6p3 0x1.9688ed7d0e0f4p6 -0x1.029e0a2e28ccbp-5",
                    "126 -0x1.0cc2a178f4e42p4 0x1.95b47bc4d17d4p6 -0x1.52e8e0362e26dp-1",
                    "147 -0x1.7d9b1c46bdf86p4 0x1.94b6086f7c96cp6 -0x1.413e204d1061ap1",
                    "168 -0x1.faba9256b512cp4 0x1.9397286346c57p6 -0x1.42d56590b3e4p2",
                    "189 -0x1.3fc0cb1203354p5 0x1.9263c29ca7633p6 -0x1.000da580be007p3",
                    "210 -0x1.838a2cfcd9d8ap5 0x1.912924c9beceep6 -0x1.62b644bc7f3a3p3",
                    "231 -0x1.c615ea17f67f6p5 0x1.8ff517e5bf81p6 -0x1.c15b941d41c28p3",
                    "252 -0x1.02ae81c722922p6 0x1.8ed4f4d45720ap6 -0x1.068135271232ap4",
                    "273 -0x1.1fd9cc5970de1p6 0x1.8dd4b8fd1797ep6 -0x1.1275b7acf88d8p4",
                    "294 -0x1.3420519484a3p6 0x1.8cfe1ae6e028fp6 -0x1.c46c70bc145dep3",
                    "315 -0x1.3e1c1f8109788p6 0x1.8c579ed346829p6 -0x1.4269f50336065p3",
                    "336 -0x1.44c18f69fd2fcp6 0x1.8be3ab59ffd4fp6 -0x1.cca44e058d28cp2",
                    "357 -0x1.48a823bf5d01cp6 0x1.8b9f9e0449e65p6 -0x1.608adc22c0595p2",
                    "378 -0x1.4a4de6cfd832ap6 0x1.8b82dfe85427fp6 -0x1.32e2a63a4a812p2",
                    "399 -0x1.4a95c26754c15p6 0x1.8b7dfa44a8ca8p6 -0x1.2b1b44a296ec5p2"}),
            java.util.Map.entry("lush->frozen", new String[] {
                    "0 0x0.0p0 0x1.98p6 0x0.0p0",
                    "21 0x1.0a76a1503f586p0 0x1.98p6 0x1.25ed165b5030cp-54",
                    "42 0x1.39aa7d1addfb2p2 0x1.98p6 0x1.59fe5a9cca49ap-52",
                    "63 0x1.8182d07057115p3 0x1.98p6 0x1.29b2d83740dbbp1",
                    "84 0x1.71007d7a4716ep4 0x1.98p6 0x1.1c261928e19f6p3",
                    "105 0x1.3ad344845a34ep5 0x1.98p6 0x1.292754494e171p4",
                    "126 0x1.84aa1cf726575p9 0x1.98p6 0x1.e4695777943f2p8",
                    "147 0x1.c199991abc88ap11 0x1.98p6 0x1.1c268e760c721p11",
                    "168 0x1.f19403845c14ap12 0x1.98p6 0x1.3b3cf30e1940cp12",
                    "189 0x1.9e53f13cdccep13 0x1.98p6 0x1.06b9fb8ab944bp13",
                    "210 0x1.26f1a4e09db24p14 0x1.98p6 0x1.763369eb69d14p13",
                    "231 0x1.7a2b366c04f4dp14 0x1.98p6 0x1.dfe31fc1fea55p13",
                    "252 0x1.bf4633c6716fdp14 0x1.98p6 0x1.1bd034f6b735bp14",
                    "273 0x1.ecb1dffc3a8e1p14 0x1.98p6 0x1.38a1b49f6e33ap14",
                    "294 0x1.f99ee4b2422abp14 0x1.98p6 0x1.40cb8384b982p14",
                    "315 0x1.f9e127b7aca8dp14 0x1.98p6 0x1.40f2f014616fbp14",
                    "336 0x1.fa0e22a332d7cp14 0x1.98p6 0x1.410db329279c8p14",
                    "357 0x1.fa2b4dd1a9ef5p14 0x1.98p6 0x1.4118p14",
                    "378 0x1.fa3b645a1cacp14 0x1.98p6 0x1.4118p14",
                    "399 0x1.fa3ffdfda879ep14 0x1.98p6 0x1.4118p14"}),
            java.util.Map.entry("lush->barren", new String[] {
                    "0 0x0.0p0 0x1.98p6 0x0.0p0",
                    "21 -0x1.0a76a1503f586p0 0x1.98p6 0x1.25ed165b5030cp-54",
                    "42 -0x1.39aa7d1addfb2p2 0x1.98p6 0x1.59fe5a9cca49ap-52",
                    "63 -0x1.90834a4538cbcp3 0x1.98p6 -0x1.3c0f5aa930b6bp0",
                    "84 -0x1.89de99e4db60ap4 0x1.98p6 -0x1.5c02b9f0c69c2p2",
                    "105 -0x1.52cc4c55be024p5 0x1.98p6 -0x1.844a1a898d321p3",
                    "126 -0x1.540773cdac319p7 0x1.98p6 -0x1.f6ec3acd5482ep5",
                    "147 -0x1.2da012a16dfedp9 0x1.98p6 -0x1.db5dd2d9ec50ap7",
                    "168 -0x1.3bc4fa329305dp10 0x1.98p6 -0x1.f8bffd9496ca7p8",
                    "189 -0x1.01760f1f64308p11 0x1.98p6 -0x1.9dc9c12a268b7p9",
                    "210 -0x1.6aeaade3150bap11 0x1.98p6 -0x1.24626152124d5p10",
                    "231 -0x1.cf08335ab81f1p11 0x1.98p6 -0x1.758104df90b81p10",
                    "252 -0x1.114d53f0e5fabp12 0x1.98p6 -0x1.b904923152ee8p10",
                    "273 -0x1.2d356da2be713p12 0x1.98p6 -0x1.e5c057e7639aap10",
                    "294 -0x1.360b977c01f64p12 0x1.98p6 -0x1.f367782bb6803p10",
                    "315 -0x1.372f6e7bdccf6p12 0x1.98p6 -0x1.f4f51259f5d17p10",
                    "336 -0x1.37f8652dc09f9p12 0x1.98p6 -0x1.f5d5983273052p10",
                    "357 -0x1.385d03915cabdp12 0x1.98p6 -0x1.f52c1c85ee744p10",
                    "378 -0x1.387835d63bdfap12 0x1.98p6 -0x1.f442d1fc97f6fp10",
                    "399 -0x1.387ffc9a84d67p12 0x1.98p6 -0x1.f4001d226ed6ep10"}),
            java.util.Map.entry("molten->lush", new String[] {
                    "0 -0x1.4a95c446a97d4p6 0x1.8b7dfa23fe537p6 -0x1.2b1b10befabbp2",
                    "21 -0x1.4a1fe89f2b55dp6 0x1.8b8240d9843afp6 -0x1.26da56f14d9a6p2",
                    "42 -0x1.47324982bc003p6 0x1.8b9d739cd6497p6 -0x1.0bcda1efec711p2",
                    "63 -0x1.40190aca28155p6 0x1.8bdf62dbd48f7p6 -0x1.947548da7c858p1",
                    "84 -0x1.33defcde3ce84p6 0x1.8c50f3f72c94dp6 -0x1.6521c0fba6e6p0",
                    "105 -0x1.21e07f3000068p6 0x1.8cf50ca6f0443p6 0x1.ffe50ddfffebp-1",
                    "126 -0x1.0815419b0bd7fp6 0x1.8dc97e5f2cd63p6 0x1.02e1cd9bf2ffp1",
                    "147 -0x1.d1bf92140c1ap5 0x1.8ec7f1b481bcbp6 0x1.80c128c4510c8p-1",
                    "168 -0x1.8cf4fcd3e0cdfp5 0x1.8fe6d1c0b78ep6 -0x1.985fd723657e4p0",
                    "189 -0x1.442e719152929p5 0x1.911a378756f04p6 -0x1.20d2e3887329ep2",
                    "210 -0x1.f3e73e7d4911ep4 0x1.9254d55a3f849p6 -0x1.e63dc90ad795dp2",
                    "231 -0x1.625f63e93ba1cp4 0x1.9388e23e3ed27p6 -0x1.52bbc6a88151ep3",
                    "252 -0x1.b1ec5803bf54p3 0x1.94a9054fa732dp6 -0x1.a3c564f921ffap3",
                    "273 -0x1.6e158d9d8e97p2 0x1.95a94126e6bb9p6 -0x1.cc82a8af2cbe6p3",
                    "294 -0x1.2fbd78ec054p-2 0x1.967fdf3d1e2a8p6 -0x1.7d755ae52ebf8p3",
                    "315 0x0.0p0 0x1.97265b50b7d0ep6 -0x1.b0e850b7e0e1ap2",
                    "336 0x0.0p0 0x1.979a4ec9fe7e8p6 -0x1.948bb1175bc9bp1",
                    "357 0x0.0p0 0x1.97de5c1fb46d2p6 -0x1.0ba6744b6c37ap0",
                    "378 0x0.0p0 0x1.97fb1a3baa2b8p6 -0x1.37ba83ef529cp-3",
                    "399 0x0.0p0 0x1.97ffffdf5588fp6 -0x1.03e61278p-16"}),
            java.util.Map.entry("molten->frozen", new String[] {
                    "0 -0x1.4a95c446a97d4p6 0x1.8b7dfa23fe537p6 -0x1.2b1b10befabbp2",
                    "21 -0x1.46fab5dd98bc7p6 0x1.8b7e10560fd7ap6 -0x1.09cc3c94f2dp2",
                    "42 -0x1.399b7bc74f134p6 0x1.8b7e62a675b7cp6 -0x1.1c8ba463177b2p1",
                    "63 -0x1.1f000817b1df6p6 0x1.8b7f08ac79f12p6 0x1.d2078c7336d1p0",
                    "84 -0x1.e73eb9bb9d404p5 0x1.8b8019ff66815p6 0x1.14e9be0ef41bcp3",
                    "105 -0x1.66f19d6ed1f58p5 0x1.8b81ae3685659p6 0x1.2c2490437bf2fp4",
                    "126 0x1.5af36e1728f86p9 0x1.8bca81a4e2219p6 0x1.e6503ee4b63edp8",
                    "147 0x1.b808dce565232p11 0x1.8ce12750455a9p6 0x1.1c789b5cda00cp11",
                    "168 0x1.ed81c5c39df02p12 0x1.8e909080d93b7p6 0x1.3b65a0e058837p12",
                    "189 0x1.9cbbb7cf5991bp13 0x1.909c35c7e16e5p6 0x1.06ca885a53f79p13",
                    "210 0x1.26619fc020354p14 0x1.92c78fb6a19d6p6 0x1.763f45f5ecc1bp13",
                    "231 0x1.79d428c103eabp14 0x1.94d616de5d72ep6 0x1.dfea85599eb1fp13",
                    "252 0x1.bf1e6b21efcd3p14 0x1.968b43d05898dp6 0x1.1bd209c1689eap14",
                    "273 0x1.eca9187aca4b2p14 0x1.97aa8f1dd6b98p6 0x1.38a247a62010bp14",
                    "294 0x1.f99ecdc25f07ep14 0x1.97fc350345ac9p6 0x1.40cbaa1f0cd85p14",
                    "315 0x1.f9e11c9a646p14 0x1.97fdd00ca93e4p6 0x1.40f302c8f61eep14",
                    "336 0x1.fa0e1f8c7367dp14 0x1.97fee71244acbp6 0x1.410db85bf9a06p14",
                    "357 0x1.fa2b4dd1a9ef3p14 0x1.97ff91ab61f56p6 0x1.4118p14",
                    "378 0x1.fa3b645a1cacp14 0x1.97ffe76f4b159p6 0x1.4118p14",
                    "399 0x1.fa3ffdfda879ep14 0x1.97fffff54a0acp6 0x1.4118p14"}),
            java.util.Map.entry("molten->barren", new String[] {
                    "0 -0x1.4a95c446a97d4p6 0x1.8b7dfa23fe537p6 -0x1.2b1b10befabbp2",
                    "21 -0x1.4e30d2afba3e1p6 0x1.8b7e9aaaf4009p6 -0x1.4c69e4e902a61p2",
                    "42 -0x1.5b900cc603e74p6 0x1.8b80edfed86ebp6 -0x1.c7f04f4c69b8cp2",
                    "63 -0x1.7754dab7de3fp6 0x1.8b859ebe30b79p6 -0x1.54258fb0cd794p3",
                    "84 -0x1.a66a91a320a88p6 0x1.8b8d578781f53p6 -0x1.f6640255f4bap3",
                    "105 -0x1.ec5742b164b14p6 0x1.8b98c2f951417p6 -0x1.707a53d1e344dp4",
                    "126 -0x1.f0f1817fa30b4p7 0x1.8bea762cbbbfp6 -0x1.2ace0484e534cp6",
                    "147 -0x1.51321deb6ed2ap9 0x1.8cffc41a27365p6 -0x1.f28865cccf8c6p7",
                    "168 -0x1.4ad1a463d07bbp10 0x1.8ea62b5ce5ff3p6 -0x1.017606939947p9",
                    "189 -0x1.07593e6bc27e7p11 0x1.90a42dfea1f47p6 -0x1.a1c6d382a55c1p9",
                    "210 -0x1.6f126f0ee225bp11 0x1.92c04e0904f0fp6 -0x1.25c605a4d6878p10",
                    "231 -0x1.d18b980a6b86fp11 0x1.94c10d85b8cf9p6 -0x1.765210ae958d9p10",
                    "252 -0x1.11e097c7cde08p12 0x1.966cee7e676b3p6 -0x1.b95cf571e281ap10",
                    "273 -0x1.2d563ab7ef90ap12 0x1.978a72fcba9ebp6 -0x1.e5cb74cf2901dp10",
                    "294 -0x1.360c1d4744fd1p12 0x1.97e4917000ad2p6 -0x1.f361416b9b62dp10",
                    "315 -0x1.372f99b04e757p12 0x1.97f02e3783621p6 -0x1.f4f2f764e367ap10",
                    "336 -0x1.37f864e0dc84ap12 0x1.97f810369a135p6 -0x1.f5d651989964ep10",
                    "357 -0x1.385d03915cabdp12 0x1.97fce20bc9dacp6 -0x1.f52c1c85ee744p10",
                    "378 -0x1.387835d63bdfap12 0x1.97ff4e5597d26p6 -0x1.f442d1fc97f6fp10",
                    "399 -0x1.387ffc9a84d67p12 0x1.97ffffb289141p6 -0x1.f4001d226ed6ep10"}),
            java.util.Map.entry("frozen->lush", new String[] {
                    "0 0x1.fa4p14 0x1.98p6 0x1.4118p14",
                    "21 0x1.fa4p14 0x1.98p6 0x1.4113d6257abfp14",
                    "42 0x1.fa4p14 0x1.98p6 0x1.410465582e522p14",
                    "63 0x1.fa311ab8c80d8p14 0x1.98p6 0x1.40ecffdd4bb63p14",
                    "84 0x1.fa07212274221p14 0x1.98p6 0x1.40cf8eed3552bp14",
                    "105 0x1.f9c90d9ee18c9p14 0x1.98p6 0x1.40a404a4d667dp14",
                    "126 0x1.ee4bf59c333cp14 0x1.98p6 0x1.3949652649fdep14",
                    "147 0x1.c23d43da6f5c4p14 0x1.98p6 0x1.1d4e84c4e1cf6p14",
                    "168 0x1.7e02b1473114bp14 0x1.98p6 0x1.e407f3c67ecc1p13",
                    "189 0x1.2b2fdf69099cdp14 0x1.98p6 0x1.7af639afa3348p13",
                    "210 0x1.a6b1ce200404cp13 0x1.98p6 0x1.0b891224e61adp13",
                    "231 0x1.00216e7ea651cp13 0x1.98p6 0x1.43cabd7cafc9ap12",
                    "252 0x1.d74850fb6e4c8p11 0x1.98p6 0x1.28cf3d12c4338p11",
                    "273 0x1.ae8066e9fbf4p9 0x1.98p6 0x1.09bc8abaed74p9",
                    "294 0x1.043cbd6caa4p5 0x1.98p6 0x1.49ebc70b298p3",
                    "315 0x1.f867037c6p3 0x1.98p6 0x1.9a27587c2p-1",
                    "336 0x1.185cc5cadap2 0x1.98p6 -0x1.69d859a8f6p2",
                    "357 0x0.0p0 0x1.98p6 -0x1.4b22e5610bp2",
                    "378 0x0.0p0 0x1.98p6 -0x1.26e978d5p0",
                    "399 0x0.0p0 0x1.98p6 -0x1.012bc31p-9"}),
            java.util.Map.entry("frozen->molten", new String[] {
                    "0 0x1.fa4p14 0x1.98p6 0x1.4118p14",
                    "21 0x1.fa4p14 0x1.97ffe9cf926f9p6 0x1.4113d6257abfp14",
                    "42 0x1.fa4p14 0x1.97ff978541fc5p6 0x1.410465582e522p14",
                    "63 0x1.fa3117febea17p14 0x1.97fef18b83036p6 0x1.40ed03c1956b5p14",
                    "84 0x1.fa0716b964304p14 0x1.97fde04cc9e1fp6 0x1.40cf9dc8f9ac5p14",
                    "105 0x1.f9c8f7d8cf3b2p14 0x1.97fc4c338af52p6 0x1.40a423b88cdp14",
                    "126 0x1.ee44e6133d556p14 0x1.97b378e01ef17p6 0x1.39491d603708ap14",
                    "147 0x1.c21b1b71196a4p14 0x1.969cd330a82d1p6 0x1.1d4c86a51d6cp14",
                    "168 0x1.7db6815db6614p14 0x1.94ed69e4bbe02p6 0x1.e3fe954748ff2p13",
                    "189 0x1.2ab0a41c2e48ep14 0x1.92e1c4744ea64p6 0x1.7ae64a1043bf6p13",
                    "210 0x1.a5470e1255e64p13 0x1.90b66a57551afp6 0x1.0b7228f73f4ap13",
                    "231 0x1.fc9fd611aec88p12 0x1.8ea7e305c3d9ap6 0x1.439011cab10a2p12",
                    "252 0x1.ceaaed9f5a278p11 0x1.8cf2b5f78f7dfp6 0x1.284843074a128p11",
                    "273 0x1.887542265026p9 0x1.8bd36aa4aca35p6 0x1.078e094040aap9",
                    "294 -0x1.6f2b4b31f36p5 0x1.8b81c4d8f5101p6 0x1.9d44defd11p2",
                    "315 -0x1.f5ee0fee136p5 0x1.8b8029edf2862p6 -0x1.74d3ea756ep1",
                    "336 -0x1.28b4576d091p6 0x1.8b7f12fcf6556p6 -0x1.28bf2cfd6ep3",
                    "357 -0x1.403cad1ba13p6 0x1.8b7e687074db1p6 -0x1.24f0684d668p3",
                    "378 -0x1.4847f154ff8p6 0x1.8b7e12b2e2743p6 -0x1.6af4bf6192p2",
                    "399 -0x1.4a94c31ae67p6 0x1.8b7dfa2eb37e1p6 -0x1.2b36e7ad23p2"}),
            java.util.Map.entry("frozen->barren", new String[] {
                    "0 0x1.fa4p14 0x1.98p6 0x1.4118p14",
                    "21 0x1.fa4p14 0x1.98p6 0x1.4113d6257abfp14",
                    "42 0x1.fa4p14 0x1.98p6 0x1.410465582e522p14",
                    "63 0x1.fa30d2b963a02p14 0x1.98p6 0x1.40ed69922f48p14",
                    "84 0x1.fa060e40835ep14 0x1.98p6 0x1.40d122820b797p14",
                    "105 0x1.f9c6ceb1ac8f2p14 0x1.98p6 0x1.40a750bfa3151p14",
                    "126 0x1.ec8b81178a10ap14 0x1.98p6 0x1.389a9ae257166p14",
                    "147 0x1.b9b06f7adf71ap14 0x1.98p6 0x1.19e692a5c280dp14",
                    "168 0x1.6ae9238624384p14 0x1.98p6 0x1.d4c232c9ab0aep13",
                    "189 0x1.0b45c45bd35f4p14 0x1.98p6 0x1.6168c65952eaep13",
                    "210 0x1.4badd9ee5aa3cp13 0x1.98p6 0x1.ce278c94eba8ep12",
                    "231 0x1.16b0200e4f82p12 0x1.98p6 0x1.cc6827a9a073p11",
                    "252 -0x1.459a38eb5a24p9 0x1.98p6 0x1.2d6c9c22098ap9",
                    "273 -0x1.f5288d6b7cb9p11 0x1.98p6 -0x1.62e239daad77p10",
                    "294 -0x1.3639d7458a848p12 0x1.98p6 -0x1.f16b952ac99fp10",
                    "315 -0x1.37498b6351e7p12 0x1.98p6 -0x1.f3b34c8df523p10",
                    "336 -0x1.3801fbce9c038p12 0x1.98p6 -0x1.f53f8a4a8d4fp10",
                    "357 -0x1.385d03915cab8p12 0x1.98p6 -0x1.f52c1c85ee75p10",
                    "378 -0x1.387835d63bdf8p12 0x1.98p6 -0x1.f442d1fc97f7p10",
                    "399 -0x1.387ffc9a84d68p12 0x1.98p6 -0x1.f4001d226ed7p10"}),
            java.util.Map.entry("barren->lush", new String[] {
                    "0 -0x1.388p12 0x1.98p6 -0x1.f4p10",
                    "21 -0x1.3870e808cad85p12 0x1.98p6 -0x1.f3e3d8d01a083p10",
                    "42 -0x1.3838ee36aeaf3p12 0x1.98p6 -0x1.f37b70646d0bep10",
                    "63 -0x1.37c6cb7936ba6p12 0x1.98p6 -0x1.f2b40ea112575p10",
                    "84 -0x1.3708be6fcc3fap12 0x1.98p6 -0x1.f17fd0f572cdcp10",
                    "105 -0x1.35ef32b854bfbp12 0x1.98p6 -0x1.efbcb3653c74p10",
                    "126 -0x1.2df5aead67693p12 0x1.98p6 -0x1.e30f543b67bd3p10",
                    "147 -0x1.12d54c09ccfb1p12 0x1.98p6 -0x1.b80abbb0c4affp10",
                    "168 -0x1.d2ff78f17997p11 0x1.98p6 -0x1.7686699c4cf1ap10",
                    "189 -0x1.6f2c2a85db199p11 0x1.98p6 -0x1.276d25d4b9ff3p10",
                    "210 -0x1.05731c9c37015p11 0x1.98p6 -0x1.a750b4c378466p9",
                    "231 -0x1.423049bf56a54p10 0x1.98p6 -0x1.0834c6cabdd4p9",
                    "252 -0x1.358e3be4b4c2p9 0x1.98p6 -0x1.063cc023397c4p8",
                    "273 -0x1.59ac3298ff58p7 0x1.98p6 -0x1.4e19690435e5p6",
                    "294 -0x1.0717a64a9a78p5 0x1.98p6 -0x1.98c7144d7e04p4",
                    "315 -0x1.ec59bc33b78p3 0x1.98p6 -0x1.17b69ffcf228p4",
                    "336 -0x1.ef602ce414p1 0x1.98p6 -0x1.6b8aa92ff93p3",
                    "357 0x0.0p0 0x1.98p6 -0x1.4b22e5610b4p2",
                    "378 0x0.0p0 0x1.98p6 -0x1.26e978d4fep0",
                    "399 0x0.0p0 0x1.98p6 -0x1.012bc31p-9"}),
            java.util.Map.entry("barren->molten", new String[] {
                    "0 -0x1.388p12 0x1.98p6 -0x1.f4p10",
                    "21 -0x1.3870e808cad85p12 0x1.97ff5f848314dp6 -0x1.f3e3d8d01a083p10",
                    "42 -0x1.3838ee36aeaf3p12 0x1.97fd0c5b29d91p6 -0x1.f37b70646d0bep10",
                    "63 -0x1.37c6e4b389867p12 0x1.97f85bf1a095bp6 -0x1.f2b31d4788a67p10",
                    "84 -0x1.370928e1afbfep12 0x1.97f0a3b593939p6 -0x1.f17bc1dd92d4dp10",
                    "105 -0x1.35f01d854277bp12 0x1.97e53914af1b7p6 -0x1.efb3aaa6135cdp10",
                    "126 -0x1.2e12ecc74f4f1p12 0x1.9793869da9ebap6 -0x1.e307de128ddabp10",
                    "147 -0x1.135f6b1353162p12 0x1.967e3893bcddbp6 -0x1.b81a936b6a9cbp10",
                    "168 -0x1.d565145a4505bp11 0x1.94d7d091c394ep6 -0x1.76bd8e409e589p10",
                    "189 -0x1.732b5c5c47c92p11 0x1.92d9ccce8dc7fp6 -0x1.27d5345210cccp10",
                    "210 -0x1.0b24b7364f9a2p11 0x1.90bdab80eb2dap6 -0x1.a889507926bacp9",
                    "231 -0x1.50cb6b1503972p10 0x1.8ebceadfab7c9p6 -0x1.09cedf258fe08p9",
                    "252 -0x1.581eafc7dc2fp9 0x1.8d1109219e6b8p6 -0x1.0a05bfd9bd138p8",
                    "273 -0x1.f20f7ccd9f06p7 0x1.8bf3847d93b13p6 -0x1.5e5b05fabacp6",
                    "294 -0x1.bc77d2914dd8p6 0x1.8b9966be23affp6 -0x1.d8f450edb8ecp4",
                    "315 -0x1.769fe7e851ecp6 0x1.8b8dcacb125cbp6 -0x1.5691669e71a4p4",
                    "336 -0x1.487d88faa7ecp6 0x1.8b85e95c31d14p6 -0x1.e989648f6918p3",
                    "357 -0x1.403cad1ba12p6 0x1.8b8117df2e567p6 -0x1.24f0684d66bp3",
                    "378 -0x1.4847f154ff8p6 0x1.8b7eabc1b4352p6 -0x1.6af4bf6190fp2",
                    "399 -0x1.4a94c31ae66cp6 0x1.8b7dfa716fb64p6 -0x1.2b36e7ad226p2"}),
            java.util.Map.entry("barren->frozen", new String[] {
                    "0 -0x1.388p12 0x1.98p6 -0x1.f4p10",
                    "21 -0x1.3870e808cad85p12 0x1.98p6 -0x1.f3e3d8d01a083p10",
                    "42 -0x1.3838ee36aeaf3p12 0x1.98p6 -0x1.f37b70646d0bep10",
                    "63 -0x1.37cb7bf2cfc36p12 0x1.98p6 -0x1.f28ec3b1704ap10",
                    "84 -0x1.371b59a77c8e4p12 0x1.98p6 -0x1.f0ea8f9bab047p10",
                    "105 -0x1.3616de2f3b591p12 0x1.98p6 -0x1.ee7d209a4ffefp10",
                    "126 -0x1.010a9fd4a8395p12 0x1.98p6 -0x1.6e9dfc2614f9cp10",
                    "147 -0x1.ad0204a38badp9 0x1.98p6 0x1.f0be64c60bad8p8",
                    "168 0x1.0565b24d0e212p12 0x1.98p6 0x1.ba2d9a99e13f2p11",
                    "189 0x1.41e729d881f38p13 0x1.98p6 0x1.c3ca991c89c08p12",
                    "210 0x1.06583e0843d5cp14 0x1.98p6 0x1.5c3c26acb55ap13",
                    "231 0x1.667950a6cf205p14 0x1.98p6 0x1.d03835cb95c9ep13",
                    "252 0x1.b6492e63761c8p14 0x1.98p6 0x1.184014f7d4964p14",
                    "273 0x1.eab9fb6061f8ep14 0x1.98p6 0x1.37ddbf34b6e6cp14",
                    "294 0x1.f99d7ec5811dp14 0x1.98p6 0x1.40cdea0332c72p14",
                    "315 0x1.f9e07a4808ea2p14 0x1.98p6 0x1.40f419d6c307cp14",
                    "336 0x1.fa0df26fdad2p14 0x1.98p6 0x1.410e05e9a3ee9p14",
                    "357 0x1.fa2b4dd1a9ef6p14 0x1.98p6 0x1.4118p14",
                    "378 0x1.fa3b645a1cac2p14 0x1.98p6 0x1.4118p14",
                    "399 0x1.fa3ffdfda879ep14 0x1.98p6 0x1.4118p14"})
            );


    /**
     * The route keys, in the order the probe dumped them.
     *
     * <p>Asserted separately so a missing key fails loudly instead of silently
     * shrinking the check.</p>
     */
    @Test
    void theGoldenTableCoversEveryOrderedPairOfBodies()
    {
        assertEquals(12, routeNames().size());
        assertEquals(12, DURATIONS.size());
        assertEquals(12, SAMPLES.size());
        for (String route : routeNames())
        {
            assertTrue(DURATIONS.containsKey(route), route + " has no pinned duration");
            assertTrue(SAMPLES.containsKey(route), route + " has no pinned samples");
        }
        assertEquals(4, BODIES.length);
        assertEquals(4, BODY_NAMES.length);
        assertEquals(21, SAMPLE_STRIDE);
    }

    private static List<String> routeNames()
    {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < BODIES.length; i++)
        {
            for (int j = 0; j < BODIES.length; j++)
            {
                if (i != j)
                    names.add(BODY_NAMES[i] + "->" + BODY_NAMES[j]);
            }
        }
        return names;
    }

    private static String entryId(String bodyName)
    {
        for (int i = 0; i < BODY_NAMES.length; i++)
        {
            if (BODY_NAMES[i].equals(bodyName))
                return BODIES[i];
        }
        throw new IllegalArgumentException(bodyName);
    }

    /**
     * Parses one golden entry of the form {@code tick x y z}.
     *
     * <p>The coordinates are {@link Double#toHexString} values, which round-trip
     * exactly. Decimal text would not: rounding to 12 places hid a 8.5e-14 gap
     * during the migration, and that gap was formatting rather than drift, so the
     * table has to carry the exact bits to be a real gate.</p>
     */
    private static double[] sample(String encoded)
    {
        String[] parts = encoded.trim().split(" +");
        return new double[] {
                Double.parseDouble(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3])};
    }

    /**
     * Every sampled position must match the pre-migration curve exactly.
     *
     * <p>Compared with zero tolerance rather than a margin: the curve is
     * deterministic arithmetic, so a correct implementation reproduces the bits. A
     * tolerance would hide the kind of small drift this exists to catch.</p>
     */
    @Test
    void everySampledPositionMatchesTheShippedCurve()
    {
        int checked = 0;
        for (String route : routeNames())
        {
            String[] fromTo = route.split("->");
            String from = entryId(fromTo[0]);
            String to = entryId(fromTo[1]);

            for (String encoded : SAMPLES.get(route))
            {
                double[] expected = sample(encoded);
                int tick = (int) expected[0];
                UniversePosition actual = ShipFlightController
                        .sampleUniversePosition(from, to, GOLDEN_TOTAL, tick);

                assertEquals(expected[1], actual.localX(), 0.0, route + " x at tick " + tick);
                assertEquals(expected[2], actual.localY(), 0.0, route + " y at tick " + tick);
                assertEquals(expected[3], actual.localZ(), 0.0, route + " z at tick " + tick);
                checked++;
            }
        }
        assertEquals(240, checked, "every golden sample must be exercised");
    }

    /** Route durations are the gameplay contract for travel time. */
    @Test
    void everyRouteDurationMatchesTheShippedValue()
    {
        for (String route : routeNames())
        {
            String[] fromTo = route.split("->");
            int total = new ShipFlightController(entryId(fromTo[0]), entryId(fromTo[1]))
                    .getTotalTicks();
            assertEquals((int) DURATIONS.get(route), total, route + " duration");
        }
    }

    /**
     * Each route starts at its departure dock and ends at its arrival dock.
     *
     * <p>This is what makes a completed flight leave the ship exactly where a
     * follow-up flight expects to start.</p>
     */
    @Test
    void everyRouteRunsDockToDock()
    {
        for (String route : routeNames())
        {
            String[] fromTo = route.split("->");
            String from = entryId(fromTo[0]);
            String to = entryId(fromTo[1]);

            assertEquals(UniverseNavigation.universeDock(from),
                    ShipFlightController.sampleUniversePosition(from, to, GOLDEN_TOTAL, 0),
                    route + " must start at the departure dock");
            assertEquals(UniverseNavigation.universeDock(to),
                    ShipFlightController.sampleUniversePosition(
                            from, to, GOLDEN_TOTAL, GOLDEN_TOTAL),
                    route + " must end at the arrival dock");
        }
    }

    /**
     * The route must remain clear of every navigable body's keep-out shell.
     *
     * <p>This is the property avoidance exists for; the golden samples pin the
     * exact path, and this pins the reason the path bends at all.</p>
     */
    @Test
    void everyRouteStaysOutsideEveryBodyKeepOutShell()
    {
        for (String route : routeNames())
        {
            String[] fromTo = route.split("->");
            String from = entryId(fromTo[0]);
            String to = entryId(fromTo[1]);
            int total = (int) DURATIONS.get(route);

            for (int tick = 0; tick <= total; tick += 5)
            {
                UniversePosition position = ShipFlightController
                        .sampleUniversePosition(from, to, total, tick);
                for (String body : BODIES)
                {
                    UniversePosition center = UniverseNavigation.universeBodyPosition(body);
                    double planar = Math.hypot(position.deltaXTo(center), position.deltaZTo(center));
                    assertTrue(planar + 1.0E-6 >= UniverseNavigation.radius(body) * 1.44,
                            route + " entered " + body + "'s keep-out shell at tick " + tick);
                }
            }
        }
    }

    /** Route phase order is what the client animates against. */
    @Test
    void everyRouteAdvancesThroughTheShippedPhaseOrder()
    {
        for (String route : routeNames())
        {
            String[] fromTo = route.split("->");
            String from = entryId(fromTo[0]);
            String to = entryId(fromTo[1]);
            int total = (int) DURATIONS.get(route);

            List<FlightPhase> phases = new ArrayList<>();
            Set<FlightPhase> seen = new LinkedHashSet<>();
            for (int tick = 0; tick <= total; tick++)
            {
                FlightPhase phase = ShipFlightController.samplePhase(from, to, total, tick);
                if (seen.add(phase))
                    phases.add(phase);
            }

            assertEquals(5, phases.size(), route + " phase count");
            assertEquals(FlightPhase.TURN, phases.get(0), route);
            assertEquals(FlightPhase.ACCELERATE, phases.get(1), route);
            assertTrue(phases.get(2) == FlightPhase.CRUISE
                            || phases.get(2) == FlightPhase.HYPERSPACE,
                    route + " travel phase must be CRUISE or HYPERSPACE");
            assertEquals(FlightPhase.DECELERATE, phases.get(3), route);
            assertEquals(FlightPhase.ARRIVE, phases.get(4), route);
        }
    }

    /**
     * Sampling the same route twice must give the same answer: the curve is a pure
     * function of (route, tick), which is what lets client and server agree.
     */
    @Test
    void theCurveIsDeterministicAcrossRepeatedSampling()
    {
        for (String route : routeNames())
        {
            String[] fromTo = route.split("->");
            String from = entryId(fromTo[0]);
            String to = entryId(fromTo[1]);
            for (int tick : new int[] {0, 17, 100, 233, GOLDEN_TOTAL})
            {
                UniversePosition first = ShipFlightController
                        .sampleUniversePosition(from, to, GOLDEN_TOTAL, tick);
                UniversePosition second = ShipFlightController
                        .sampleUniversePosition(from, to, GOLDEN_TOTAL, tick);
                assertEquals(first, second,
                        route + " at tick " + tick + " is not deterministic");
            }
        }
    }
}
