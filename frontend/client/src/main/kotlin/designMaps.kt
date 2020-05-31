import kotlinx.css.Color

data class ModuleTypeDesign(val fullTitle: String, val backgroundColor: Color)

val moduleTypeMap: Map<String, ModuleTypeDesign> = mapOf(
    "E1" to ModuleTypeDesign("Examination 1", Color("#ECD2E0")),
    "E2" to ModuleTypeDesign("Examination 2", Color("#ECD2E0")),
    "E3" to ModuleTypeDesign("Examination 3", Color("#ECD2E0")),
    "LA" to ModuleTypeDesign("Laboratory work", Color("#C9C900")),
    "ŽP" to ModuleTypeDesign("Verbal test", Color("#E8D700")),
    "IN" to ModuleTypeDesign("Individual task", Color("#DBDCDD")),
    "IR" to ModuleTypeDesign("Engineering project", Color("#BD98C5")),
    "KL" to ModuleTypeDesign("Colloquium", Color("#76B0E1")),
    "PA" to ModuleTypeDesign("Work placement report", Color("#E8E9F3")),
    "RA" to ModuleTypeDesign("Project report", Color("#BBB5BD")),
    "TE" to ModuleTypeDesign("Mid-term examination", Color("#DFDF66"))
)

data class ModuleScienceTypeDesign(val backgroundColor: Color)

val moduleScienceTypeMap: Map<Char, ModuleScienceTypeDesign> = mapOf(
    'T' to ModuleScienceTypeDesign(Color("#ABDDD0")),
    'S' to ModuleScienceTypeDesign(Color("#FDF0D6")),
    'P' to ModuleScienceTypeDesign(Color("#E4D7DC"))
)