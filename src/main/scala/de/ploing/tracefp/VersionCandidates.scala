package de.ploing.tracefp


class CandidateBundle {
  val candidates = collection.mutable.Map[(String,String), Set[String]]()

  def addCandidate(group : String, artifact : String, version : String) {
    val fqArtifact = (group,artifact)
    if (candidates.contains(fqArtifact)) {
      candidates(fqArtifact) = candidates(fqArtifact) + version
    } else {
      candidates(fqArtifact) = Set(version)
    }
  }
}


class VersionCandidates extends CandidateBundle {
  def addCandidateBundle(bundle : CandidateBundle) {
    bundle.candidates.foreach { case (lib,versions) =>
      if (candidates.contains(lib)) {
        candidates(lib) = candidates(lib) & versions
      } else {
        candidates(lib) = versions
      }
    }
  }
}
